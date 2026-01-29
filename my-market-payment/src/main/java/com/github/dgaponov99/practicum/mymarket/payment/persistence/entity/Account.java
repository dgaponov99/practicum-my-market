package com.github.dgaponov99.practicum.mymarket.payment.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigInteger;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table("account")
public class Account {

    @Id
    @Column("account_id")
    private Long id;

    private long balance;

}
