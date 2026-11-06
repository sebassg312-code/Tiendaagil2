# Tienda Ágil – Java Desktop (NetBeans)

Proyecto Maven Java Swing. Abrir la carpeta en Apache NetBeans y ejecutar `com.tiendaagil.TiendaAgilApp`. Requiere JDK 21+.

## Incluye
- Prototipo desktop de login, dashboard, inventario, POS, caja, reportes y configuración.
- Persistencia local de demostración y backup local.
- Script SQL de diseño físico.
- Documentación de estado y propuesta de arquitectura para APF3.

## Importante sobre el estado
La aplicación base concentra gran parte del código en `TiendaAgilApp.java`; la separación completa MVC/DAO, JDBC/MySQL, logging Logback, exportación XLSX con Apache POI y pruebas automatizadas todavía deben integrarse y verificarse. El informe técnico adjunto distingue lo implementado de lo pendiente.

## Ejecutar
1. Abrir NetBeans → File → Open Project → seleccionar esta carpeta (pom.xml).
2. Configurar JDK 21.
3. Run Project.

Usuarios demo: admin/admin123 y cajero1/cajero123. Cambiar para cualquier uso fuera de demostración.
