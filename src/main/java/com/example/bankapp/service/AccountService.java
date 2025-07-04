package com.example.bankapp.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;

@Service
public class AccountService implements UserDetailsService {
	
	@Autowired
	PasswordEncoder passwordEncoder;
	
	
	@Autowired
	private AccountRepository accountRepository;
	
	@Autowired
	private TransactionRepository transactionRepository;
	
	
	public Account findAccountByUsername(String username) {
		return accountRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("Account not found with username: " + username));
	}
	
	public Account registerAccount(String username, String passowrd) {
		if (accountRepository.findByUsername(username).isPresent()) {
			throw new RuntimeException("Username already exists" + username);
		}
		
		Account account = new Account();
		account.setUsername(username);
		account.setPassword(passwordEncoder.encode(passowrd));
		account.setBalance(BigDecimal.ZERO); // Set initial balance to 0.0
		return accountRepository.save(account);
	}
	
	public void deposit(Account account, BigDecimal amount) {
		account.setBalance(account.getBalance().add(amount));
		accountRepository.save(account);
		
		Transaction transaction = new Transaction(
				"Deposit",
				amount,
				LocalDateTime.now(),
				account
				
		);
		transactionRepository.save(transaction);
		
	}
	
	public void withdraw (Account account, BigDecimal amount) {
		if (account.getBalance().compareTo(amount) < 0) {
			throw new RuntimeException("Insufficient balance for withdrawal");
		}
		account.setBalance(account.getBalance().subtract(amount));
		accountRepository.save(account);
		
		Transaction transaction = new Transaction(
				"Withdrawal",
				amount,
				LocalDateTime.now(),
				account
		);
		transactionRepository.save(transaction);
		
	}
	
	public List<Transaction> getTransactionHistory(Account account) {
		return transactionRepository.findByAccountId(account.getId());
	}
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Account account = findAccountByUsername(username);
		
		if (account == null) {
			throw new UsernameNotFoundException("Account not found with username: " + username);
		}
		
		return new Account(
				account.getUsername(),
				account.getPassword(),
				account.getBalance(),
				account.getTransactions(),
				account.getAuthorities()
		);
	}
		
	public Collection<? extends GrantedAuthority> getAuthorities() {
			return Arrays.asList(new SimpleGrantedAuthority("User"));
	}
	
	public void transferAmount(Account fromAccount, String toUsername, BigDecimal amount) {
		
		if(fromAccount.getBalance().compareTo(amount) < 0) {
			throw new RuntimeException("Insufficient balance for transfer");
		}
		
		Account toAccount = accountRepository.findByUsername(toUsername)
				.orElseThrow(() -> new RuntimeException("Account not found with username: " + toUsername));
		
		fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
		accountRepository.save(fromAccount);
		
		toAccount.setBalance(toAccount.getBalance().add(amount));
		accountRepository.save(toAccount);
		
		
		Transaction debitTransaction = new Transaction(
				"Transfer Out to "+ toAccount.getUsername(),
				amount, 
				LocalDateTime.now(),
				fromAccount
		);
		transactionRepository.save(debitTransaction);
		
		Transaction creditTransaction = new Transaction(
				"Transfer In to "+ fromAccount.getUsername(),
				amount, 
				LocalDateTime.now(),
				toAccount
		);
		transactionRepository.save(creditTransaction);
		
	}
	
}
