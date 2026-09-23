# Uso de Inteligencia Artificial en este proyecto

Durante el desarrollo de este reto usé IA (Claude, versión web gratuita) como apoyo en las siguientes partes:

- **Análisis del documento del reto**: para desglosarlo punto por punto y tener claro qué se esperaba en cada sección antes de empezar a codificar.
- **Limpieza de datos**: apoyo en la normalización del dataset de transacciones (nulos, formatos de fecha y montos inconsistentes) para el pipeline ETL.
- **Redacción de documentación**: ayuda para estructurar y pulir el README y este informe técnico.
- **Redacción de mensajes de commit**: apoyo para escribir mensajes de commit claros y consistentes a lo largo del desarrollo.
- **Verificación del funcionamiento**: apoyo para armar los datos de prueba (los INSERT de las cuentas semilla) y las pruebas manuales del endpoint con curl — transferencia exitosa, idempotencia repitiendo la misma clave, y error de saldo insuficiente — confirmando en la base de datos que los saldos quedaban correctos en cada caso.
