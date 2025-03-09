package com.mi.bankx.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mi.bankx.model.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
	
	Account findByCustomerIdAndType(Long customerId, String type);
	
}
