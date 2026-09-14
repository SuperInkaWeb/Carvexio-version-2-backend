package com.saas.automotriz.model;


public enum BusinessStatus {
    PENDING,   // recién registrado, esperando aprobación
    APPROVED,  // aprobado, visible en marketplace
    REJECTED,  // rechazado
    BLOCKED    // bloqueado por mal comportamiento
}