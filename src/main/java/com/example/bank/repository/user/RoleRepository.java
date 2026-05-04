package com.example.bank.repository.user;

import com.example.bank.entity.user.Role;
import com.example.bank.projection.UserBasicProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository <Role, Long> {
    Optional<Role> findByName (String name);

}
