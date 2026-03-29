package com.example.bank.entity.wallet;

import com.example.bank.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "card_holders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardHolder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * FK tới card
     */
    @Column(name = "card_id", nullable = false, unique = true)
    private Long cardId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "address_line", nullable = false, length = 255)
    private String addressLine;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country", nullable = false, length = 10)
    private String country;
}