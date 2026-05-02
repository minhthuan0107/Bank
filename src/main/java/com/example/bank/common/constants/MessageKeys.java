package com.example.bank.common.constants;

public class MessageKeys {
    public static final String SESSION_INVALID   = "auth.session.invalid";
    public static final String USER_ACCOUNT_DELETED = "auth.account-deleted";
    public static final String USER_ACCOUNT_LOCKED = "auth.account-locked";
    public static final String SESSION_EXPIRED   = "auth.session.expired";
    public static final String ACCOUNT_NOT_FOUND = "account.not.found";
    public static final String RESET_TOKEN_INVALID = "reset.token.invalid";
    public static final String RESET_SESSION_EXPIRED = "reset.session.expired";
    public static final String SESSION_REFRESHED = "auth.session.refreshed";




    public static final String AUTH_SESSION_NOT_FOUND = "auth.session.not-found";
    public static final String WRONG_CREDENTIALS = "auth.wrong-credentials";
    public static final String AUTHENTICATION_FAILED = "auth.authentication-failed";
    public static final String ACCESS_DENIED = "auth.access-denied";
    public static final String ROLE_DEFAULT_NOT_FOUND = "role.default-not-found";
    public static final String AUTHENTICATED_USER_NOT_FOUND = "auth.authenticated-user-not-found";

    public static final String VALIDATION_USERNAME_NOT_BLANK = "validation.username.not-blank";
    public static final String VALIDATION_USERNAME_INVALID = "validation.username.invalid";
    public static final String VALIDATION_PASSWORD_NOT_BLANK = "validation.password.not-blank";
    public static final String VALIDATION_USERNAME_MIN_LENGTH = "validation.username.min-length";
    public static final String VALIDATION_PASSWORD_COMPLEXITY = "validation.password.complexity";
    public static final String VALIDATION_CONFIRM_PASSWORD_NOT_BLANK = "validation.confirm-password.not-blank";
    public static final String VALIDATION_PASSWORD_LENGTH = "validation.password.length";
    public static final String VALIDATION_INVALID_ENUM = "validation.invalid-enum";
    public static final String VALIDATION_OTP_PURPOSE_NOT_NULL = "validation.otp.purpose.not-null";
    public static final String VALIDATION_AMOUNT_NOT_NULL = "validation.amount.not_null";
    public static final String VALIDATION_AMOUNT_MIN = "validation.amount.min";

    public static final String REQUEST_INVALID = "request.invalid";

    public static final String LOGIN_SUCCESS = "auth.login.success";
    public static final String SIGNUP_SUCCESS = "auth.signup.success";

    public static final String VALIDATION_EMAIL_NOT_BLANK  = "validation.email.not-blank";
    public static final String VALIDATION_EMAIL_INVALID = "validation.email.invalid";
    public static final String VALIDATION_PASSWORD_MISMATCH = "validation.password.mismatch";

    public static final String ACCOUNT_ALREADY_EXISTS = "account.already-exists";

    public static final String OTP_INVALID_PURPOSE = "otp.invalid-purpose";
    public static final String OTP_ENQUEUED = "otp.enqueued";
    public static final String OTP_COOLDOWN_ACTIVE = "otp.cooldown-active";
    public static final String RATE_LIMIT_NETWORK = "otp.rate-limit.network";
    public static final String RATE_LIMIT_HOURLY = "otp.rate-limit.hourly";
    public static final String VALIDATION_OTP_REQUIRED = "validation.otp.required";

    public static final String OTP_EXPIRED_OR_NOT_FOUND = "auth.otp.expired_or_not_found";
    public static final String OTP_INVALID = "auth.otp.invalid";
    public static final String EMAIL_ALREADY_EXISTS = "auth.email.already_exists";

    // ===== CURRENCY =====
    public static final String VALIDATION_CURRENCY_NOT_BLANK = "validation.currency.not_blank";
    public static final String VALIDATION_CURRENCY_MAX_LENGTH = "validation.currency.max_length";
    // ===== FEE PERCENT =====
    public static final String VALIDATION_FEE_PERCENT_NOT_NULL = "validation.fee_percent.not_null";
    public static final String VALIDATION_FEE_PERCENT_MIN = "validation.fee_percent.min";
    public static final String VALIDATION_FEE_PERCENT_MAX = "validation.fee_percent.max";
    // ===== MIN AMOUNT =====
    public static final String VALIDATION_MIN_AMOUNT_NOT_NULL = "validation.min_amount.not_null";
    public static final String VALIDATION_MIN_AMOUNT_MIN = "validation.min_amount.min";
    // ===== MAX AMOUNT =====
    public static final String VALIDATION_MAX_AMOUNT_MIN = "validation.max_amount.min";
    public static final String VALIDATION_MAX_AMOUNT_GREATER_THAN_MIN = "validation.max_amount.greater_than_min";
    public static final String DEPOSIT_SETTINGS_CREATED = "deposit.settings.created";
    public static final String DEPOSIT_SETTINGS_ALREADY_EXISTS = "deposit.settings.already_exists";

    public static final String VALIDATION_NETWORK_NOT_BLANK = "validation.network.not_blank";
    public static final String VALIDATION_NETWORK_MAX_LENGTH = "validation.network.max_length";

    public static final String VALIDATION_DEPOSIT_ADDRESS_NOT_BLANK = "validation.deposit_address.not_blank";
    public static final String VALIDATION_DEPOSIT_ADDRESS_MAX_LENGTH = "validation.deposit_address.max_length";

    public static final String VALIDATION_DISPLAY_ORDER_NOT_NULL = "validation.display_order.not_null";

    public static final String DEPOSIT_ADDRESS_ALREADY_EXISTS = "deposit.address.already_exists";
    public static final String DEPOSIT_ADDRESS_CREATED = "deposit.address.created";

    public static final String DEPOSIT_CONFIG_FETCHED = "deposit.config.fetched";
    public static final String DEPOSIT_SETTINGS_NOT_FOUND = "deposit.settings.not_found";
    public static final String WALLET_NOT_FOUND = "wallet.not_found";



    public static final String DEPOSIT_AMOUNT_BELOW_MIN = "deposit.amount_below_min";
    public static final String DEPOSIT_ADDRESS_NOT_FOUND = "deposit.address_not_found";
    public static final String ADMIN_NOT_FOUND = "admin.not_found";

    public static final String DEPOSIT_PREVIEW_FETCHED = "deposit.preview_fetched";


    public static final String DEPOSIT_ORDER_RATE_LIMIT = "deposit.order_rate_limit";

    public static final String DEPOSIT_ORDER_CREATED = "deposit.order.created";
    public static final String VALIDATION_CURRENCY_NOT_NULL = "validation.currency.not_null";

    public static final String DEPOSIT_PROOF_REQUIRED = "deposit.proof.required";
    public static final String DEPOSIT_PROOF_MAX_IMAGES = "deposit.proof.max_images";
    public static final String DEPOSIT_ORDER_NOT_FOUND = "deposit.order.not_found";
    public static final String DEPOSIT_ORDER_INVALID_STATUS = "deposit.order.invalid_status";

    public static final String FILE_EMPTY = "file.empty";
    public static final String FILE_TOO_LARGE = "file.too_large";
    public static final String FILE_INVALID_TYPE = "file.invalid_type";
    public static final String FILE_INVALID_IMAGE = "file.invalid_image";
    public static final String FILE_READ_ERROR = "file.read_error";
    public static final String DEPOSIT_PROOF_UPLOADED_SUCCESS = "deposit.proof.uploaded.success";
    public static final String DEPOSIT_PROOF_ALREADY_UPLOADED ="deposit.proof.already_uploaded";
    public static final String DEPOSIT_ORDER_LIST_SUCCESS = "deposit.order.list.success";


    public static final String VALIDATION_WITHDRAW_ADDRESS_NOT_BLANK = "validation.withdraw.address.not.blank";
    public static final String VALIDATION_WITHDRAW_ADDRESS_MAX_LENGTH = "validation.withdraw.address.max.length";
    public static final String VALIDATION_WITHDRAW_AMOUNT_MIN = "validation.withdraw.amount.min";


    public static final String WITHDRAW_ORDER_ALREADY_PENDING = "wallet.withdraw.order.already.pending";
    public static final String WITHDRAW_DAILY_LIMIT_REACHED = "wallet.withdraw.daily.limit.reached";
    public static final String USER_NOT_FOUND = "user.not.found";

    public static final String WITHDRAW_ORDER_NOT_FOUND = "wallet.withdraw.order.not.found";
    public static final String WITHDRAW_INVALID_STATUS = "wallet.withdraw.invalid.status";
    public static final String WITHDRAW_OTP_EXPIRED = "wallet.withdraw.otp.expired";
    public static final String WITHDRAW_OTP_INVALID = "wallet.withdraw.otp.invalid";
    public static final String WITHDRAW_OTP_TOO_MANY_ATTEMPTS = "wallet.withdraw.otp.too.many.attempts";
    public static final String WITHDRAW_ORDER_CREATED = "withdraw.order.created";
    public static final String WITHDRAW_OTP_CONFIRMED = "withdraw.otp.confirmed";
    public static final String OTP_RESENT = "otp.resent";
    public static final String WITHDRAW_ORDER_LIST_SUCCESS = "withdraw.order.list.success";
    public static final String WALLET_INSUFFICIENT_BALANCE = "wallet.balance.insufficient";
    public static final String VALIDATION_STATUS_NOT_NULL = "validation.withdraw.status.not_null";
    public static final String VALIDATION_ADMIN_NOTE_MAX_LENGTH = "validation.withdraw.admin_note.max_length";
    public static final String INVALID_PAGE_NUMBER = "common.invalid.page.number";

    public static final String WITHDRAW_TOO_MANY_REQUESTS = "withdraw.too_many_requests";
    public static final String WITHDRAW_STATUS_UPDATED = "withdraw.status.updated";
    public static final String ADMIN_WITHDRAW_ORDER_LIST_SUCCESS = "admin.withdraw.order.list.success";

    public static final String TOO_MANY_REQUESTS = "common.too.many.requests";


    // ===== CARD =====
    public static final String VALIDATION_CARD_BIN_REQUIRED = "validation.card.bin.required";
    public static final String VALIDATION_CARD_NAME_REQUIRED = "validation.card.name.required";
    public static final String VALIDATION_CARD_AMOUNT_REQUIRED = "validation.card.amount.required";
    public static final String VALIDATION_CARD_AMOUNT_MIN = "validation.card.amount.min";

    // ===== HOLDER =====
    public static final String VALIDATION_FIRST_NAME_REQUIRED = "validation.cardholder.first_name.required";
    public static final String VALIDATION_LAST_NAME_REQUIRED = "validation.cardholder.last_name.required";
    public static final String VALIDATION_ADDRESS_REQUIRED = "validation.cardholder.address.required";
    public static final String VALIDATION_CITY_REQUIRED = "validation.cardholder.city.required";
    public static final String VALIDATION_COUNTRY_REQUIRED = "validation.cardholder.country.required";
    public static final String VALIDATION_STATE_REQUIRED = "validation.cardholder.state.required";
    public static final String VALIDATION_POSTAL_CODE_REQUIRED = "validation.cardholder.postal_code.required";

    public static final String CARD_CREATED = "card.created.success";

    public static final String INSUFFICIENT_BALANCE = "wallet.insufficient_balance";
    public static final String CARD_LIMIT_EXCEEDED = "card.limit.exceeded";
    public static final String INVALID_BIN = "card.bin.invalid";

    public static final String SLASH_CREATE_CARD_FAILED = "slash.create_card.failed";
    public static final String SLASH_GET_CARD_FAILED = "slash.card.get.failed";
    public static final String CARD_WITHDRAW_SUCCESS="card.withdraw.success";
    public static final String CARD_TOPUP_SUCCESS = "card.topup.success";

    public static final String CARD_NOT_FOUND = "card.not.found";
    public static final String TOPUP_FAILED = "wallet.topup.failed";
    public static final String CARD_TOPUP_IN_PROGRESS = "card.topup.in.progress";

    public static final String SLASH_UPDATE_LIMIT_FAILED = "slash.card.update.limit.failed";
    public static final String CARD_WITHDRAW_FAILED = "card.withdraw.failed";
    public static final String CARD_WITHDRAW_IN_PROGRESS = "card.withdraw.in_progress";
    public static final String WALLET_UPDATE_FAILED = "wallet.update.failed";
    public static final String INSUFFICIENT_CARD_BALANCE = "card.balance.insufficient";
    public static final String CARD_UPDATE_FAILED ="card.update.failed";
    public static final String VALIDATION_CARD_NOTE_MAX_LENGTH="validation.card.note.max_length";
    public static final String CARD_LIST_SUCCESS = "card.list.success";

    public static final String VALIDATION_ORDER_NO_REQUIRED = "validation.order_no.required";
    public static final String VALIDATION_ORDER_NO_MAX_LENGTH = "validation.order_no.max_length";
    public static final String VALIDATION_OTP_INVALID = "validation.otp.invalid";

    public static final String WITHDRAW_OTP_STILL_VALID = "withdraw.otp.still.valid";
    public static final String BALANCE_FETCH_SUCCESS = "balance.fetch.success";
    public static final String INVALID_BALANCE = "wallet.balance.invalid";
    public static final String CARD_ALREADY_ACTIVE = "card.already-active";
    public static final String CARD_ALREADY_BLOCKED = "card.already-blocked";
    public static final String CARD_HAS_PENDING_TRANSACTION = "card.has-pending-transaction";

    public static final String SLASH_UPDATE_CARD_FAILED = "card.update-failed";

    public static final String CARD_LOCK_SUCCESS = "card.lock-success";
    public static final String CARD_UNLOCK_SUCCESS = "card.unlock-success";

    public static final String INVALID_TIME_RANGE = "validation.time-range.invalid";
    public static final String CARD_DASHBOARD_SUCCESS = "card.dashboard.success";
    public static final String WITHDRAW_DASHBOARD_SUCCESS = "withdraw.dashboard.success";

    public static final String DEPOSIT_DASHBOARD_SUCCESS = "deposit.dashboard.success";

    public static final String DASHBOARD_SUCCESS = "dashboard.success";

    public static final String SLASH_GET_TRANSACTION_FAILED = "slash.get_transaction.failed";



    public static final String VALIDATION_MIN_SPENT_INVALID = "validation.cashback.min-spent.invalid";
    public static final String VALIDATION_AMOUNT_FORMAT = "validation.amount.format";
    public static final String VALIDATION_PERCENT_FORMAT = "validation.percent.format";
    public static final String VALIDATION_CASHBACK_PERCENT_INVALID = "validation.cashback.percent.invalid";
    // ===== Cashback Required Validation =====
    public static final String VALIDATION_CASHBACK_PERCENT_REQUIRED = "validation.cashback.percent.required";
    public static final String VALIDATION_MIN_SPENT_REQUIRED = "validation.cashback.min-spent.required";
    public static final String INVALID_CASHBACK_RANGE = "validation.cashback.range.invalid";
    public static final String CASHBACK_RULE_CREATED = "cashback.rule.created";
    public static final String CASHBACK_PERCENT_ALREADY_EXISTS = "cashback.percent.already-exists";
    public static final String CASHBACK_RULE_NOT_FOUND = "cashback.rule.not-found";
    public static final String CASHBACK_RULE_UPDATED = "cashback.rule.updated";
    public static final String CASHBACK_RULE_DELETED = "cashback.rule.deleted";
    public static final String CASHBACK_DASHBOARD_SUCCESS = "cashback.dashboard.success";
    public static final String CASHBACK_RULE_LIST_SUCCESS = "cashback.rule.list.success";
    public static final String VALIDATION_USER_IDS_REQUIRED = "validation.user.ids.required";
    public static final String VALIDATION_USER_ID_REQUIRED = "validation.user.id.required";


    public static final String INVALID_MONTH = "invalid.month";
    public static final String CASHBACK_REFUND_LIST_SUCCESS = "cashback.refund.list.success";
    public static final String CASHBACK_BATCH_APPROVED = "cashback.batch.approved";
    public static final String CASHBACK_HISTORY_LIST_SUCCESS = "cashback.history.list.success";
    public static final String ADMIN_USER_LIST_SUCCESS = "admin.user.list.success";
    public static final String CARD_OPEN_LIMIT_UPDATE_SUCCESS = "card.open.limit.update.success";

    public static final String VALIDATION_CARD_OPEN_LIMIT_REQUIRED = "validation.card.open.limit.required";
    public static final String VALIDATION_CARD_OPEN_LIMIT_INVALID = "validation.card.open.limit.invalid";

    public static final String USER_CARD_LIMIT_UPDATE_NOT_ALLOWED = "user.card.limit.update.not.allowed";
    public static final String CARD_TRANSACTION_LIST_SUCCESS = "card.transaction.list.success";


    public static final String DEPOSIT_INVALID_STATUS = "deposit.invalid.status";
    public static final String DEPOSIT_STATUS_UPDATED = "deposit.status.updated";

    public static final String ADMIN_DEPOSIT_ORDER_LIST_SUCCESS = "admin.deposit.order.list.success";



}
