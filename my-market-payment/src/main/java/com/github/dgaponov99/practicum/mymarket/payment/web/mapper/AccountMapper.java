package com.github.dgaponov99.practicum.mymarket.payment.web.mapper;

import com.github.dgaponov99.practicum.mymarket.payment.config.MapstructConfiguration;
import com.github.dgaponov99.practicum.mymarket.payment.persistence.entity.Account;
import com.github.dgaponov99.practicum.mymarket.payment.web.dto.AccountDTO;
import org.mapstruct.Mapper;

@Mapper(config = MapstructConfiguration.class)
public abstract class AccountMapper {

    public abstract AccountDTO accountToDto(Account account);

    public abstract Account dtoToAccount(AccountDTO accountDTO);

}
