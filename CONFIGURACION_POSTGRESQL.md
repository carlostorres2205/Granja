# Conectar Granja a PostgreSQL

La aplicación usa la base `Granja` en `localhost:5432` y el usuario `postgres` por defecto.
En IntelliJ abre **Run → Edit Configurations**, selecciona la configuración de
`GranjaApplication` y agrega estas variables de entorno:

```text
GRANJA_DB_PASSWORD=tu_contraseña_de_postgresql
```

Si el servidor o usuario son distintos, agrega también:

```text
GRANJA_DB_URL=jdbc:postgresql://localhost:5432/Granja
GRANJA_DB_USER=postgres
```

No escribas la contraseña de PostgreSQL en archivos del proyecto. Ejecuta `GranjaApplication`.
Si aún no hay usuarios, crea la primera cuenta administradora con una contraseña de
al menos 8 caracteres. Después inicia sesión y abre **Animales y lotes**. Al guardar un animal, el
registro debe aparecer en la tabla y permanecer después de reiniciar la aplicación.

La contraseña de la cuenta de la aplicación se guarda como un hash protegido;
es distinta de `GRANJA_DB_PASSWORD`, que sirve para conectar con PostgreSQL.
