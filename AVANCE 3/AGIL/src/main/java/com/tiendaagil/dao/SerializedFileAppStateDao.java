package com.tiendaagil.dao;

import com.tiendaagil.TiendaAgilApp;
import java.io.*;
import java.nio.file.*;

/** DAO local basado en serialización; permite operar offline durante el avance. */
public final class SerializedFileAppStateDao implements AppStateDao {
    private final Path file;
    public SerializedFileAppStateDao(Path file) { this.file = file; }
    @Override public TiendaAgilApp.AppState load() throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            Object value = in.readObject();
            if (!(value instanceof TiendaAgilApp.AppState state)) throw new IOException("Formato de datos no reconocido");
            return state;
        }
    }
    @Override public void save(TiendaAgilApp.AppState state) throws IOException {
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tmp))) { out.writeObject(state); }
        try { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException ex) { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING); }
    }
    public Path getFile() { return file; }
}
