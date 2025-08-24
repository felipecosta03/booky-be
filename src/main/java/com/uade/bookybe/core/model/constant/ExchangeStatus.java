package com.uade.bookybe.core.model.constant;

public enum ExchangeStatus {
  /** Intercambio recién creado, esperando respuesta del propietario */
  PENDING,

  /** Intercambio aceptado por el propietario */
  ACCEPTED,

  /** Intercambio rechazado por el propietario */
  REJECTED,

  /** El propietario hizo una contraoferta */
  COUNTERED,

  /** Intercambio cancelado por el solicitante */
  CANCELLED,

  /** Intercambio completado exitosamente */
  COMPLETED
}
