package com.tiendaagil.dao;

import com.tiendaagil.TiendaAgilApp;
import java.io.IOException;

/** Contrato DAO para cargar y guardar el estado persistente de la aplicación. */
public interface AppStateDao {
    TiendaAgilApp.AppState load() throws IOException, ClassNotFoundException;
    void save(TiendaAgilApp.AppState state) throws IOException;
}
