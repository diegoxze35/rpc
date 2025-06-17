# Servicio gRPC para Transferencia de Archivos Grandes
Descripción
Este servicio gRPC implementa un sistema eficiente para transferir archivos de gran tamaño (4GB+) utilizando streaming de datos en chunks. Está diseñado para manejar transferencias de archivos grandes con un consumo mínimo de memoria en el servidor.

## Características a este commit
- Streaming de datos en chunks (4MB)
- Control de concurrencia para evitar sobrecarga del servidor

## Requisitos técnicos
- Java 17+
- Spring Boot 3.0+
- gRPC Java 1.50+

# Estructura del Protocolo gRPC
```proto
syntax = "proto3";
option java_multiple_files = true;
option java_package = "file.service.proto";

message GetFileResponse {
  bytes data = 1;
}

message GetFileRequest {
  string fileName = 1;
}

service FileService {
  rpc getFile(GetFileRequest) returns (stream GetFileResponse);
}
```
## Configuración adicional de la JVM
-XX:MaxDirectMemorySize=8G

## Uso con grpcurl
```
grpcurl -d '{\"fileName\":\"file.zip\"}' -plaintext localhost:9090 FileService.getFile
```
