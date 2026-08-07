# Validación de la interfaz CRUD

## Responsable

Isaac Hurel (`ijhurel-lab`).

## Componentes implementados

- Formulario para crear y editar gastos.
- Spinner alimentado desde la tabla de categorías.
- Listado mediante RecyclerView y CardView.
- Visualización del nombre de la categoría obtenido con INNER JOIN.
- Pantalla de detalle.
- Diálogo de confirmación antes de eliminar.
- Actualización del listado después de crear, editar o eliminar.

## Validaciones

- Descripción obligatoria.
- Monto numérico y mayor que cero.
- Fecha válida en formato AAAA-MM-DD.
- Categoría obligatoria.
- Confirmación previa a la eliminación.

## Verificación técnica

El proyecto fue compilado mediante `gradlew.bat assembleDebug` y obtuvo `BUILD SUCCESSFUL`.

El emulador del equipo de Isaac presenta limitaciones por los recursos disponibles del computador. Por este motivo, las pruebas visuales y funcionales completas se realizarán también en los equipos de Bryan Salame y Fabricio Urdin.

## Resultado esperado

La aplicación debe permitir iniciar sesión, listar los ocho gastos iniciales, crear, consultar, editar y eliminar registros, además de conservar los datos almacenados en SQLite después de cerrar y volver a abrir la aplicación.
