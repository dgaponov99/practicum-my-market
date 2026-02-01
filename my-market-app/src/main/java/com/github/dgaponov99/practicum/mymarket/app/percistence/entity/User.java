package com.github.dgaponov99.practicum.mymarket.app.percistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {

    @Id
    @Column("user_id")
    private Long id;

    private String username;
    private String password;
    @Column("account_id")
    private long accountId;
    private boolean disabled;
}
