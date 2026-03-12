package com.example.bank.service.otp.impl;

import com.example.bank.common.config.properties.OtpProperties;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.constants.RedisKeys;
import com.example.bank.common.context.AuthContext;
import com.example.bank.common.exception.auth.OtpException;
import com.example.bank.dto.request.otp.OtpRequest;
import com.example.bank.dto.response.otp.OtpEnqueuedResponse;
import com.example.bank.enums.otp.OtpPurpose;
import com.example.bank.service.mail.MailService;
import com.example.bank.service.otp.OtpService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {

        private final StringRedisTemplate redis;
        private final OtpProperties otpProperties;
        private final MailService mailService;

        @Override
        public OtpEnqueuedResponse handleOtpRequest(OtpRequest req, HttpServletRequest httpReq) {

            AuthContext ctx = AuthContext.from(httpReq);

            final String email = req.getEmail();
            final OtpPurpose purpose = req.getPurpose();

            final String masked = maskEmail(email);

            // ===== Validate purpose =====
            if (purpose != OtpPurpose.SIGNUP &&
                    purpose != OtpPurpose.SIGNIN &&
                    purpose != OtpPurpose.RESET) {

                log.warn(
                        "OTP-FAIL reason=INVALID_PURPOSE purpose={} email={} ip={}",
                        purpose,
                        masked,
                        ctx.getIp()
                );

                throw new OtpException(MessageKeys.OTP_INVALID_PURPOSE);
            }

            // ===== Redis keys =====
            final String idKey = "email:" + email;

            final String otpKey = RedisKeys.otpValueKey(idKey);
            final String cdKey = RedisKeys.otpCooldownKey(idKey);
            final String hourKey = RedisKeys.otpHourlyKey(idKey);
            final String ipHourKey = RedisKeys.otpIpHourlyKey(ctx.getIp());

            // ===== Cooldown =====
            if (Boolean.TRUE.equals(redis.hasKey(cdKey))) {
                Long remain = redis.getExpire(cdKey, TimeUnit.SECONDS);
                log.warn(
                        "OTP-FAIL reason=COOLDOWN_ACTIVE purpose={} email={} ip={}",
                        purpose,
                        masked,
                        ctx.getIp()
                );

                throw new OtpException(
                        MessageKeys.OTP_COOLDOWN_ACTIVE,
                        remain != null ? remain.intValue() : null
                );
            }

            // ===== IP limit =====
            Long ipCnt = redis.opsForValue().increment(ipHourKey);
            if (ipCnt != null && ipCnt == 1) {
                redis.expire(ipHourKey, 3600, TimeUnit.SECONDS);
            }
            if (ipCnt != null && ipCnt > otpProperties.getLimit().getIpHourly()) {
                Long remain = redis.getExpire(ipHourKey, TimeUnit.SECONDS);
                log.warn(
                        "OTP-FAIL reason=RATE_LIMIT scope=IP_LIMIT purpose={} email={} ip={}",
                        purpose,
                        masked,
                        ctx.getIp()
                );

                throw new OtpException(
                        MessageKeys.RATE_LIMIT_NETWORK,
                        remain != null ? remain.intValue() : null
                );
            }

            // ===== Email hourly limit =====
            Long hCnt = redis.opsForValue().increment(hourKey);

            if (hCnt != null && hCnt == 1) {
                redis.expire(hourKey, 3600, TimeUnit.SECONDS);
            }

            if (hCnt != null && hCnt > otpProperties.getLimit().getPerHour()) {

                Long remain = redis.getExpire(hourKey, TimeUnit.SECONDS);

                log.warn(
                        "OTP-FAIL reason=RATE_LIMIT scope=HOUR_LIMIT purpose={} email={} ip={}",
                        purpose,
                        masked,
                        ctx.getIp()
                );

                throw new OtpException(
                        MessageKeys.RATE_LIMIT_HOURLY,
                        remain != null ? remain.intValue() : null
                );
            }

            // ===== Generate OTP =====
            final String otp = genOtp6();

            // ===== Save OTP =====
            redis.opsForValue().set(
                    otpKey,
                    otp,
                    otpProperties.getTtlSeconds(),
                    TimeUnit.SECONDS
            );

            // ===== Set cooldown =====
            redis.opsForValue().set(
                    cdKey,
                    "1",
                    otpProperties.getCooldownSeconds(),
                    TimeUnit.SECONDS
            );

            // ===== Send email =====
            mailService.sendOtp(email, otp);

            log.info(
                    "OTP-SUCCESS purpose={} email={} ip={}",
                    purpose,
                    masked,
                    ctx.getIp()
            );

            return new OtpEnqueuedResponse(
                    null,
                    masked,
                    otpProperties.getTtlSeconds(),
                    otpProperties.getCooldownSeconds()
            );
        }

        private String genOtp6() {
            int otp = ThreadLocalRandom.current().nextInt(100000, 999999);
            return String.valueOf(otp);
        }

        private String maskEmail(String email) {

            int at = email.indexOf("@");

            if (at <= 2) return email;

            return email.substring(0, 2)
                    + "***"
                    + email.substring(at);
        }
    }

