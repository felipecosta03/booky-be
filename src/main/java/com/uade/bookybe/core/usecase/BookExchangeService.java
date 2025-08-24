package com.uade.bookybe.core.usecase;

import com.uade.bookybe.core.model.BookExchange;
import com.uade.bookybe.core.model.constant.ExchangeStatus;
import java.util.List;
import java.util.Optional;

public interface BookExchangeService {

  /** Creates a new book exchange request */
  Optional<BookExchange> createExchange(
      String requesterId, String ownerId, List<String> ownerBookIds, List<String> requesterBookIds);

  /** Gets all exchanges for a user (both as requester and owner) */
  List<BookExchange> getUserExchanges(String userId);

  /** Gets exchanges by user and status */
  List<BookExchange> getUserExchangesByStatus(String userId, ExchangeStatus status);

  /** Gets a specific exchange by ID */
  Optional<BookExchange> getExchangeById(String exchangeId);

  /** Updates the status of an exchange (accept, reject, complete, cancel) */
  Optional<BookExchange> updateExchangeStatus(
      String exchangeId, String userId, ExchangeStatus status);

  /** Creates a counter offer for an existing exchange */
  Optional<BookExchange> createCounterOffer(
      String exchangeId, String userId, List<String> ownerBookIds, List<String> requesterBookIds);

  /** Gets exchanges where user is the requester */
  List<BookExchange> getExchangesAsRequester(String userId);

  /** Gets exchanges where user is the owner */
  List<BookExchange> getExchangesAsOwner(String userId);

  /** Gets pending exchanges count for a user */
  long getPendingExchangesCount(String userId);
}
