package com.github.dgaponov99.practicum.mymarket.payment.persistence.repository;

import com.github.dgaponov99.practicum.mymarket.payment.persistence.entity.Account;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends ReactiveCrudRepository<Account, Long> {
}
