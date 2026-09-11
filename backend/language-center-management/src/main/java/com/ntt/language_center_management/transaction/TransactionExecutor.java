package com.ntt.language_center_management.transaction;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransactionExecutor {

  @Transactional
  public <T> T required(Supplier<T> action) {
    return action.get();
  }

}
