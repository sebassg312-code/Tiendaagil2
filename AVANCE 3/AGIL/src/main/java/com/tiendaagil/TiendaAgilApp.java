package com.tiendaagil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import com.tiendaagil.dao.AppStateDao;
import com.tiendaagil.dao.SerializedFileAppStateDao;
import com.tiendaagil.service.SaleCalculator;
import com.tiendaagil.service.InventoryRules;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aplicación de escritorio Tienda Ágil.
 * La interfaz coordina los módulos; reglas de venta e inventario se delegan a servicios
 * y la persistencia local se realiza mediante la abstracción DAO.
 *
 * Tienda Ágil - demostración de escritorio.
 * Persistencia local en un archivo binario para trabajar sin Internet.
 * La integración con MySQL se documenta aparte y no está conectada en esta versión.
 */
public class TiendaAgilApp {
    private static final Color NAVY = new Color(29, 41, 57);
    private static final Color BLUE = new Color(39, 100, 235);
    private static final Color BG = new Color(245, 247, 250);
    private static final Color TEXT = new Color(35, 43, 55);
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static AppState state;
    private static final Logger LOG = LoggerFactory.getLogger(TiendaAgilApp.class);
    private static Path dataFile = Paths.get(System.getProperty("user.home"), ".tienda-agil", "data.bin");
    private static AppStateDao stateDao = new SerializedFileAppStateDao(dataFile);
    private static User currentUser;
    private static JFrame frame;
    private static JPanel content;
    private static String storeName = "Tienda Ágil";
    private static final java.util.List<CartLine> cart = new ArrayList<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            // Colores legibles por defecto para todos los controles Swing, incluidos diálogos.
            UIManager.put("Label.foreground", TEXT);
            UIManager.put("TextField.foreground", TEXT);
            UIManager.put("TextField.background", Color.WHITE);
            UIManager.put("TextField.caretForeground", TEXT);
            UIManager.put("PasswordField.foreground", TEXT);
            UIManager.put("PasswordField.background", Color.WHITE);
            UIManager.put("PasswordField.caretForeground", TEXT);
            UIManager.put("TextArea.foreground", TEXT);
            UIManager.put("TextArea.background", Color.WHITE);
            UIManager.put("TextArea.caretForeground", TEXT);
            UIManager.put("ComboBox.foreground", TEXT);
            UIManager.put("ComboBox.background", Color.WHITE);
            UIManager.put("Button.foreground", TEXT);
            UIManager.put("Button.background", Color.WHITE);
            UIManager.put("Table.foreground", TEXT);
            UIManager.put("Table.background", Color.WHITE);
            UIManager.put("Table.selectionForeground", Color.WHITE);
            UIManager.put("Table.selectionBackground", BLUE);
            UIManager.put("TableHeader.foreground", TEXT);
            UIManager.put("TableHeader.background", new Color(235,238,243));
            try {
                Files.createDirectories(dataFile.getParent());
                stateDao = new SerializedFileAppStateDao(dataFile);
                state = loadState();
                showLogin();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "No se pudo iniciar Tienda Ágil:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static AppState loadState() throws Exception {
        if (Files.exists(dataFile)) {
            try { return stateDao.load(); }
            catch (Exception ex) {
                LOG.error("No se pudo leer el archivo local; se conservará una copia del archivo dañado", ex);
                Path corrupt = dataFile.resolveSibling("data-corrupt-" + System.currentTimeMillis() + ".bin");
                try { Files.move(dataFile, corrupt); } catch (IOException moveEx) { LOG.warn("No se pudo mover el archivo dañado", moveEx); }
            }
        }
        AppState s = new AppState();
        s.users.add(User.create("admin", "Administrador General", "ADMINISTRADOR", "admin123"));
        s.users.add(User.create("cajero1", "Cajero Principal", "CAJERO", "cajero123"));
        s.products.add(new Product(1, "775000000001", "Gaseosa Cola 500 ml", "Bebidas", 3.50, 50, 10, true));
        s.products.add(new Product(2, "775000000002", "Papas Clásicas", "Snacks", 4.00, 30, 5, true));
        s.products.add(new Product(3, "775000000003", "Leche Entera 1 L", "Lácteos", 5.50, 20, 5, true));
        s.products.add(new Product(4, "775000000004", "Agua Mineral 625 ml", "Bebidas", 2.00, 100, 20, true));
        s.products.add(new Product(5, "775000000005", "Galletas de Chocolate", "Snacks", 3.00, 40, 10, true));
        s.nextProductId = 6;
        persist(s);
        return s;
    }

    private static void persist() {
        try { persist(state); }
        catch (Exception ex) { LOG.error("No se pudieron guardar los cambios", ex); JOptionPane.showMessageDialog(frame, "No se pudieron guardar los cambios localmente:\n" + ex.getMessage(), "Error de almacenamiento", JOptionPane.ERROR_MESSAGE); }
    }

    private static void persist(AppState s) throws IOException {
        stateDao.save(s);
        LOG.info("Estado local guardado: {} productos, {} ventas", s.products.size(), s.sales.size());
    }

    private static void showLogin() {
        JFrame login = new JFrame("Tienda Ágil | Inicio de sesión");
        login.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        login.setSize(460, 430); login.setLocationRelativeTo(null); login.setResizable(false);
        JPanel outer = new JPanel(new GridBagLayout()); outer.setBackground(BG);
        JPanel card = new JPanel(); card.setPreferredSize(new Dimension(350, 330)); card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(220,225,232)), new EmptyBorder(24, 28, 24, 28)));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel logo = new JLabel("TIENDA ÁGIL", SwingConstants.CENTER); logo.setFont(new Font("SansSerif", Font.BOLD, 26)); logo.setForeground(NAVY); logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitle = new JLabel("Ingresa tus credenciales", SwingConstants.CENTER); subtitle.setForeground(new Color(110,119,130)); subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        JTextField username = new JTextField(); JPasswordField password = new JPasswordField();
        styleField(username); styleField(password);
        JButton enter = button("Iniciar sesión", BLUE); enter.setAlignmentX(Component.CENTER_ALIGNMENT); enter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        JLabel info = new JLabel("Modo local · Funciona sin Internet", SwingConstants.CENTER); info.setForeground(new Color(110,119,130)); info.setFont(new Font("SansSerif", Font.PLAIN, 11)); info.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(logo); card.add(Box.createVerticalStrut(6)); card.add(subtitle); card.add(Box.createVerticalStrut(25));
        card.add(label("Usuario")); card.add(username); card.add(Box.createVerticalStrut(13)); card.add(label("Contraseña")); card.add(password); card.add(Box.createVerticalStrut(22)); card.add(enter); card.add(Box.createVerticalStrut(15)); card.add(info);
        outer.add(card); login.setContentPane(outer);
        Runnable auth = () -> {
            String u = username.getText().trim(); char[] pw = password.getPassword();
            User found = state.users.stream().filter(x -> x.active && x.username.equalsIgnoreCase(u) && x.matches(pw)).findFirst().orElse(null);
            Arrays.fill(pw, '\0');
            if (found == null) { JOptionPane.showMessageDialog(login, "Usuario o contraseña incorrectos.", "Acceso denegado", JOptionPane.WARNING_MESSAGE); return; }
            currentUser = found; login.dispose(); showMain();
        };
        enter.addActionListener(e -> auth.run()); password.addActionListener(e -> auth.run());
        login.setVisible(true);
    }

    private static void showMain() {
        frame = new JFrame(storeName + " | Sistema de gestión"); frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); frame.setMinimumSize(new Dimension(1020, 680)); frame.setSize(1180, 760); frame.setLocationRelativeTo(null);
        frame.addWindowListener(new java.awt.event.WindowAdapter() { @Override public void windowClosing(java.awt.event.WindowEvent e) { logout(); } });
        JPanel root = new JPanel(new BorderLayout()); root.setBackground(BG);
        JPanel sidebar = new JPanel(); sidebar.setBackground(NAVY); sidebar.setPreferredSize(new Dimension(205, 700)); sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS)); sidebar.setBorder(new EmptyBorder(18, 12, 12, 12));
        JLabel brand = new JLabel("Tienda Ágil"); brand.setForeground(Color.WHITE); brand.setFont(new Font("SansSerif", Font.BOLD, 21)); brand.setBorder(new EmptyBorder(0, 8, 3, 8));
        JLabel role = new JLabel(currentUser.fullName + " · " + currentUser.role); role.setForeground(new Color(191,201,216)); role.setFont(new Font("SansSerif", Font.PLAIN, 10)); role.setBorder(new EmptyBorder(0,8,20,2));
        sidebar.add(brand); sidebar.add(role);
        addNav(sidebar, "Dashboard", () -> dashboard());
        addNav(sidebar, "Punto de Venta", () -> pos());
        if (isAdmin()) addNav(sidebar, "Inventario", () -> inventory());
        addNav(sidebar, "Caja y Turnos", () -> cashDesk());
        if (isAdmin()) addNav(sidebar, "Reportes", () -> reports());
        if (isAdmin()) addNav(sidebar, "Configuración", () -> settings());
        if (isAdmin()) addNav(sidebar, "Usuarios", () -> users());
        sidebar.add(Box.createVerticalGlue());
        JLabel storage = new JLabel("● Almacenamiento local"); storage.setForeground(new Color(157,224,180)); storage.setFont(new Font("SansSerif", Font.PLAIN, 11)); storage.setBorder(new EmptyBorder(8,8,12,0)); sidebar.add(storage);
        addNav(sidebar, "Cerrar sesión", () -> logout());
        content = new JPanel(new BorderLayout()); content.setBackground(BG); content.setBorder(new EmptyBorder(22, 24, 22, 24));
        root.add(sidebar, BorderLayout.WEST); root.add(content, BorderLayout.CENTER); frame.setContentPane(root); dashboard(); frame.setVisible(true);
    }

    private static boolean isAdmin() { return currentUser != null && "ADMINISTRADOR".equals(currentUser.role); }
    private static void addNav(JPanel sidebar, String name, Runnable action) {
        JButton b = new JButton(name);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setForeground(Color.WHITE);
        b.setBackground(NAVY);
        // Force Swing to paint the intended dark navigation background; some LAFs
        // otherwise render a white button while retaining the white foreground text.
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setRolloverEnabled(false);
        b.setBorder(new EmptyBorder(10, 12, 10, 6));
        b.setFont(new Font("SansSerif", Font.PLAIN, 13));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        b.addMouseListener(new java.awt.event.MouseAdapter(){
            public void mouseEntered(java.awt.event.MouseEvent e){b.setBackground(new Color(50,67,88));}
            public void mouseExited(java.awt.event.MouseEvent e){b.setBackground(NAVY);}
        });
        sidebar.add(b);
        sidebar.add(Box.createVerticalStrut(4));
    }

    private static void dashboard() {
        content.removeAll(); content.setLayout(new BorderLayout(0,18));
        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false); top.add(title("Dashboard"), BorderLayout.WEST); top.add(new JLabel("Usuario: " + currentUser.fullName), BorderLayout.EAST); content.add(top, BorderLayout.NORTH);
        JPanel body = new JPanel(); body.setOpaque(false); body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        double sales = state.sales.stream().filter(s -> sameDay(s.date, LocalDate.now())).mapToDouble(s -> s.total).sum();
        long tx = state.sales.stream().filter(s -> sameDay(s.date, LocalDate.now())).count();
        long low = state.products.stream().filter(p -> p.active && InventoryRules.isLowStock(p.stock,p.minStock)).count();
        JPanel cards = new JPanel(new GridLayout(1,4,12,12)); cards.setOpaque(false);
        cards.add(metric("Ventas de hoy", money(sales), BLUE)); cards.add(metric("Transacciones", String.valueOf(tx), new Color(22,163,74))); cards.add(metric("Productos activos", String.valueOf(state.products.stream().filter(p->p.active).count()), new Color(147,51,234))); cards.add(metric("Alertas de stock", String.valueOf(low), new Color(220,38,38)));
        body.add(cards); body.add(Box.createVerticalStrut(18));
        JPanel mid = new JPanel(new GridLayout(1,2,12,12)); mid.setOpaque(false);
        mid.add(panelWithTitle("Productos con stock bajo", lowStockTable())); mid.add(panelWithTitle("Últimas ventas", recentSalesTable())); body.add(mid);
        body.add(Box.createVerticalStrut(18)); JLabel note = new JLabel("Modo de trabajo: aplicación de escritorio · persistencia local · no requiere conexión para registrar ventas."); note.setForeground(new Color(92,104,120)); body.add(note);
        content.add(body, BorderLayout.CENTER); refresh();
    }

    private static JPanel lowStockTable() {
        String[] cols={"Producto","Stock","Mínimo"}; DefaultTableModel m=nonEditable(cols);
        state.products.stream().filter(p->p.active && p.stock<=p.minStock).forEach(p->m.addRow(new Object[]{p.name,p.stock,p.minStock}));
        return scrollTable(m);
    }
    private static JPanel recentSalesTable() {
        String[] cols={"Venta","Fecha","Total"}; DefaultTableModel m=nonEditable(cols);
        state.sales.stream().sorted((a,b)->b.date.compareTo(a.date)).limit(6).forEach(s->m.addRow(new Object[]{"V-"+s.id,s.date.format(DT),money(s.total)})); return scrollTable(m);
    }

    private static void inventory() {
        if (!isAdmin()) { denied(); return; }
        content.removeAll(); content.setLayout(new BorderLayout(0,14)); content.add(title("Gestión de inventario"),BorderLayout.NORTH);
        JPanel main = new JPanel(new BorderLayout(0,10)); main.setOpaque(false);
        JPanel controls = new JPanel(new BorderLayout(8,0)); controls.setOpaque(false); JTextField search=new JTextField(); styleField(search); controls.add(search,BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); actions.setOpaque(false); JButton add=button("Agregar producto",BLUE); JButton edit=button("Editar",new Color(22,163,74)); JButton deactivate=button("Inactivar",new Color(220,38,38)); actions.add(add); actions.add(edit); actions.add(deactivate); controls.add(actions,BorderLayout.EAST); main.add(controls,BorderLayout.NORTH);
        String[] cols={"ID","Código de barras","Producto","Categoría","Precio","Stock","Stock mínimo","Estado"}; DefaultTableModel model=nonEditable(cols); JTable table=new JTable(model); styleTable(table); main.add(new JScrollPane(table),BorderLayout.CENTER); content.add(main,BorderLayout.CENTER);
        Runnable fill=()->{model.setRowCount(0); String q=search.getText().toLowerCase(Locale.ROOT); state.products.stream().filter(p->(p.name+" "+p.barcode+" "+p.category).toLowerCase(Locale.ROOT).contains(q)).forEach(p->model.addRow(new Object[]{p.id,p.barcode,p.name,p.category,money(p.price),p.stock,p.minStock,p.active?"Activo":"Inactivo"}));};
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){public void insertUpdate(javax.swing.event.DocumentEvent e){fill.run();}public void removeUpdate(javax.swing.event.DocumentEvent e){fill.run();}public void changedUpdate(javax.swing.event.DocumentEvent e){fill.run();}});
        add.addActionListener(e->{Product p=productDialog(null);if(p!=null){p.id=state.nextProductId++;state.products.add(p);persist();fill.run();}});
        edit.addActionListener(e->{Product old=selectedProduct(table);if(old==null)return;Product changed=productDialog(old.copy());if(changed!=null){copyProduct(changed,old);persist();fill.run();}});
        deactivate.addActionListener(e->{Product p=selectedProduct(table);if(p==null)return; p.active=false;persist();fill.run();});
        fill.run(); refresh();
    }

    private static Product selectedProduct(JTable table) { int row=table.getSelectedRow(); if(row<0){JOptionPane.showMessageDialog(frame,"Selecciona un producto primero.");return null;} int id=(int)table.getValueAt(table.convertRowIndexToModel(row),0); return state.products.stream().filter(p->p.id==id).findFirst().orElse(null); }
    private static Product productDialog(Product initial) {
        JTextField barcode=new JTextField(initial==null?"":initial.barcode), name=new JTextField(initial==null?"":initial.name), price=new JTextField(initial==null?"":String.valueOf(initial.price)), stock=new JTextField(initial==null?"0":String.valueOf(initial.stock)), min=new JTextField(initial==null?"5":String.valueOf(initial.minStock));
        JComboBox<String> category=new JComboBox<>(new String[]{"Bebidas","Snacks","Lácteos","Alimentos","Limpieza","Cuidado personal","Otros"}); if(initial!=null)category.setSelectedItem(initial.category);
        JPanel p=new JPanel(new GridLayout(0,2,8,8)); p.add(new JLabel("Código de barras"));p.add(barcode);p.add(new JLabel("Nombre"));p.add(name);p.add(new JLabel("Categoría"));p.add(category);p.add(new JLabel("Precio de venta"));p.add(price);p.add(new JLabel("Stock actual"));p.add(stock);p.add(new JLabel("Stock mínimo"));p.add(min);
        int res=JOptionPane.showConfirmDialog(frame,p,initial==null?"Nuevo producto":"Editar producto",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE); if(res!=JOptionPane.OK_OPTION)return null;
        try {String bc=barcode.getText().trim(), nm=name.getText().trim(); if(StringUtils.isBlank(bc)||StringUtils.isBlank(nm))throw new IllegalArgumentException("Código y nombre son obligatorios."); boolean dup=state.products.stream().anyMatch(x->x.barcode.equals(bc)&&(initial==null||x.id!=initial.id));if(dup)throw new IllegalArgumentException("Ya existe un producto con ese código de barras."); double pr=Double.parseDouble(price.getText().replace(',','.'));int st=Integer.parseInt(stock.getText()),mi=Integer.parseInt(min.getText()); if(pr<0||st<0||mi<0)throw new IllegalArgumentException("Precio y cantidades deben ser positivos."); Product out=initial==null?new Product(0,bc,nm,(String)category.getSelectedItem(),pr,st,mi,true):initial;out.barcode=bc;out.name=nm;out.category=(String)category.getSelectedItem();out.price=pr;out.stock=st;out.minStock=mi;return out;}
        catch(Exception ex){JOptionPane.showMessageDialog(frame,"Revisa los datos: "+ex.getMessage(),"Datos inválidos",JOptionPane.WARNING_MESSAGE);return productDialog(initial);}
    }
    private static void copyProduct(Product src, Product dest){dest.barcode=src.barcode;dest.name=src.name;dest.category=src.category;dest.price=src.price;dest.stock=src.stock;dest.minStock=src.minStock;dest.active=src.active;}

    private static void pos() {
        content.removeAll(); content.setLayout(new BorderLayout(0,12)); content.add(title("Punto de Venta (POS)"),BorderLayout.NORTH);
        JPanel root=new JPanel(new GridLayout(1,2,12,0));root.setOpaque(false);
        JPanel left=new JPanel(new BorderLayout(0,8));left.setOpaque(false);JTextField search=new JTextField();search.setToolTipText("Buscar por nombre o código de barras");styleField(search);left.add(search,BorderLayout.NORTH);String[] pc={"Código","Producto","Precio","Stock"};DefaultTableModel pm=nonEditable(pc);JTable products=new JTable(pm);styleTable(products);left.add(new JScrollPane(products),BorderLayout.CENTER);JButton add=button("Agregar al carrito",BLUE);left.add(add,BorderLayout.SOUTH);
        JPanel right=new JPanel(new BorderLayout(0,8));right.setOpaque(false);JLabel cartTitle=new JLabel("Carrito de venta");cartTitle.setFont(new Font("SansSerif",Font.BOLD,16));right.add(cartTitle,BorderLayout.NORTH);String[] cc={"Producto","Cant.","Precio","Subtotal"};DefaultTableModel cm=nonEditable(cc);JTable cartTable=new JTable(cm);styleTable(cartTable);right.add(new JScrollPane(cartTable),BorderLayout.CENTER);
        JPanel bottom=new JPanel();bottom.setBackground(Color.WHITE);bottom.setBorder(new EmptyBorder(12,12,12,12));bottom.setLayout(new BoxLayout(bottom,BoxLayout.Y_AXIS));JLabel subtotalLabel=new JLabel();subtotalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);JLabel igvLabel=new JLabel();igvLabel.setAlignmentX(Component.LEFT_ALIGNMENT);JLabel totalLabel=new JLabel();totalLabel.setFont(new Font("SansSerif",Font.BOLD,20));totalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);JPanel cartActions=new JPanel(new GridLayout(1,3,6,0));cartActions.setOpaque(false);JButton qty=button("Cambiar cantidad",new Color(90,104,120));JButton remove=button("Quitar",new Color(220,38,38));JButton pay=button("Cobrar",new Color(22,163,74));cartActions.add(qty);cartActions.add(remove);cartActions.add(pay);bottom.add(subtotalLabel);bottom.add(Box.createVerticalStrut(4));bottom.add(igvLabel);bottom.add(Box.createVerticalStrut(5));bottom.add(totalLabel);bottom.add(Box.createVerticalStrut(10));bottom.add(cartActions);right.add(bottom,BorderLayout.SOUTH);
        root.add(left);root.add(right);content.add(root,BorderLayout.CENTER);
        Runnable fillProducts=()->{pm.setRowCount(0);String q=search.getText().trim().toLowerCase(Locale.ROOT);state.products.stream().filter(p->p.active&&(p.name+" "+p.barcode).toLowerCase(Locale.ROOT).contains(q)).forEach(p->pm.addRow(new Object[]{p.barcode,p.name,money(p.price),p.stock}));};
        Runnable fillCart=()->{cm.setRowCount(0);double sub=0;for(CartLine line:cart){double lineTotal=SaleCalculator.subtotal(line.product.price,line.quantity);sub+=lineTotal;cm.addRow(new Object[]{line.product.name,line.quantity,money(line.product.price),money(lineTotal)});}double igv=SaleCalculator.tax(sub,0.18);subtotalLabel.setText("Subtotal: "+money(sub));igvLabel.setText("IGV referencial (18%): "+money(igv));totalLabel.setText("Total: "+money(SaleCalculator.total(sub,igv)));};
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){public void insertUpdate(javax.swing.event.DocumentEvent e){fillProducts.run();}public void removeUpdate(javax.swing.event.DocumentEvent e){fillProducts.run();}public void changedUpdate(javax.swing.event.DocumentEvent e){fillProducts.run();}});
        add.addActionListener(e->{int row=products.getSelectedRow();if(row<0){JOptionPane.showMessageDialog(frame,"Selecciona un producto.");return;}String bc=(String)products.getValueAt(products.convertRowIndexToModel(row),0);Product p=state.products.stream().filter(x->x.barcode.equals(bc)).findFirst().orElse(null);if(p==null||p.stock<=0){JOptionPane.showMessageDialog(frame,"Producto sin stock disponible.");return;}CartLine line=cart.stream().filter(x->x.product.id==p.id).findFirst().orElse(null);if(line==null)cart.add(new CartLine(p,1));else if(line.quantity+1>p.stock){JOptionPane.showMessageDialog(frame,"La cantidad supera el stock disponible.");return;}else line.quantity++;fillCart.run();});
        // Los lectores USB suelen comportarse como teclado y enviar Enter al terminar el código.
        search.addActionListener(e->{String scanned=search.getText().trim();if(scanned.isEmpty())return;int found=-1;for(int i=0;i<pm.getRowCount();i++){if(scanned.equals(String.valueOf(pm.getValueAt(i,0)))){found=i;break;}}if(found>=0){products.setRowSelectionInterval(found,found);add.doClick();search.setText("");}else{Toolkit.getDefaultToolkit().beep();JOptionPane.showMessageDialog(frame,"No se encontró el código escaneado.");}});
        qty.addActionListener(e->{int row=cartTable.getSelectedRow();if(row<0){JOptionPane.showMessageDialog(frame,"Selecciona un producto del carrito.");return;}CartLine line=cart.get(row);String ans=JOptionPane.showInputDialog(frame,"Cantidad (1 - "+line.product.stock+"):",String.valueOf(line.quantity));if(ans==null)return;try{int q=Integer.parseInt(ans);if(q<1||q>line.product.stock)throw new NumberFormatException();line.quantity=q;fillCart.run();}catch(Exception ex){JOptionPane.showMessageDialog(frame,"Cantidad inválida o superior al stock.");}});
        remove.addActionListener(e->{int row=cartTable.getSelectedRow();if(row>=0){cart.remove(row);fillCart.run();}});
        pay.addActionListener(e->{if(cart.isEmpty()){JOptionPane.showMessageDialog(frame,"El carrito está vacío.");return;}Turn turno=activeTurn();if(turno==null){JOptionPane.showMessageDialog(frame,"Primero debes abrir un turno en Caja y Turnos.","Caja cerrada",JOptionPane.WARNING_MESSAGE);return;}checkout(fillCart);});
        fillProducts.run();fillCart.run();refresh();
    }

    private static void checkout(Runnable redrawCart) {
        String[] methods={"Efectivo","Tarjeta (registro declarativo)","QR (registro declarativo)"};String method=(String)JOptionPane.showInputDialog(frame,"Selecciona el método de pago:","Cobrar venta",JOptionPane.QUESTION_MESSAGE,null,methods,methods[0]);if(method==null)return;
        double subtotal=cart.stream().mapToDouble(x->SaleCalculator.subtotal(x.product.price,x.quantity)).sum();double igv=SaleCalculator.tax(subtotal,0.18),total=SaleCalculator.total(subtotal,igv);double received=total,change=0;
        if(method.startsWith("Efectivo")){String input=JOptionPane.showInputDialog(frame,"Total: "+money(total)+"\nMonto recibido:",String.format(Locale.US,"%.2f",total));if(input==null)return;try{received=Double.parseDouble(input.replace(',','.'));if(received<total){JOptionPane.showMessageDialog(frame,"El monto recibido es menor al total.");return;}change=SaleCalculator.change(received,total);}catch(Exception e){JOptionPane.showMessageDialog(frame,"Monto inválido.");return;}}
        for(CartLine line:cart){try{InventoryRules.validateSaleQuantity(line.product.stock,line.quantity);}catch(IllegalArgumentException ex){JOptionPane.showMessageDialog(frame,"Stock insuficiente para: "+line.product.name);return;}}
        Sale sale=new Sale();sale.id=state.nextSaleId++;sale.date=LocalDateTime.now();sale.username=currentUser.username;sale.turnId=activeTurn().id;sale.subtotal=subtotal;sale.igv=igv;sale.total=total;sale.paymentMethod=method;sale.received=received;sale.change=change;
        for(CartLine line:cart){line.product.stock=InventoryRules.stockAfterSale(line.product.stock,line.quantity);sale.items.add(new SaleItem(line.product.id,line.product.name,line.quantity,line.product.price,SaleCalculator.subtotal(line.product.price,line.quantity)));}
        state.sales.add(sale);persist();StringBuilder ticket=new StringBuilder();ticket.append(storeName).append("\nCOMPROBANTE DE VENTA\n").append("Venta V-").append(sale.id).append("\n").append(sale.date.format(DT)).append("\nCajero: ").append(currentUser.fullName).append("\n--------------------------------\n");for(SaleItem item:sale.items)ticket.append(item.name).append(" x").append(item.quantity).append("  ").append(money(item.subtotal)).append("\n");ticket.append("--------------------------------\nSubtotal: ").append(money(subtotal)).append("\nIGV referencial: ").append(money(igv)).append("\nTOTAL: ").append(money(total)).append("\nPago: ").append(method).append("\nRecibido: ").append(money(received)).append("\nVuelto: ").append(money(change)).append("\n\nComprobante de demostración");JTextArea area=new JTextArea(ticket.toString(),22,32);area.setEditable(false);area.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));JScrollPane sc=new JScrollPane(area);int opts=JOptionPane.showOptionDialog(frame,sc,"Venta completada",JOptionPane.DEFAULT_OPTION,JOptionPane.INFORMATION_MESSAGE,null,new String[]{"Imprimir…","Cerrar"},"Cerrar");if(opts==0){try{area.print();}catch(Exception ex){JOptionPane.showMessageDialog(frame,"No se pudo imprimir: "+ex.getMessage());}}
        cart.clear();pos();
    }

    private static void cashDesk() {
        content.removeAll();content.setLayout(new BorderLayout(0,14));content.add(title("Caja registradora y turnos"),BorderLayout.NORTH);
        JPanel top=new JPanel(new GridLayout(1,2,12,0));top.setOpaque(false);JPanel op=panelWithTitle("Operación de turno",new JPanel());op.setLayout(new BorderLayout(0,12));JPanel fields=new JPanel(new GridLayout(0,2,8,8));fields.setOpaque(false);JTextField opening=new JTextField();JTextField physical=new JTextField();styleField(opening);styleField(physical);fields.add(new JLabel("Dinero inicial (S/)"));fields.add(opening);fields.add(new JLabel("Efectivo físico al cierre (S/)"));fields.add(physical);op.add(fields,BorderLayout.CENTER);JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT,6,0));buttons.setOpaque(false);JButton open=button("Iniciar turno",new Color(22,163,74));JButton close=button("Cerrar turno",new Color(220,38,38));JButton calc=button("Calcular monto",BLUE);buttons.add(open);buttons.add(close);buttons.add(calc);op.add(buttons,BorderLayout.SOUTH);
        JPanel summary=new JPanel();summary.setBackground(Color.WHITE);summary.setBorder(new EmptyBorder(15,15,15,15));summary.setLayout(new BoxLayout(summary,BoxLayout.Y_AXIS));summary.add(label("Resumen del turno"));JLabel status=new JLabel();JLabel expected=new JLabel();JLabel diff=new JLabel();summary.add(Box.createVerticalStrut(10));summary.add(status);summary.add(expected);summary.add(diff);top.add(op);top.add(panelWithTitle("Resumen",summary));
        String[] cols={"Turno","Cajero","Inicio","Fin","Monto inicial","Esperado","Físico","Diferencia","Estado"};DefaultTableModel tm=nonEditable(cols);JTable table=new JTable(tm);styleTable(table);JPanel deskBody=new JPanel(new BorderLayout(0,12));deskBody.setOpaque(false);deskBody.add(top,BorderLayout.NORTH);deskBody.add(new JScrollPane(table),BorderLayout.CENTER);content.add(deskBody,BorderLayout.CENTER);
        Runnable fill=()->{tm.setRowCount(0);state.turns.stream().sorted((a,b)->Integer.compare(b.id,a.id)).forEach(t->tm.addRow(new Object[]{t.id,t.username,t.start.format(DT),t.end==null?"—":t.end.format(DT),money(t.opening),money(t.expected),t.physical==null?"—":money(t.physical),t.difference==null?"—":money(t.difference),t.open?"Abierto":"Cerrado"}));Turn a=activeTurn();if(a==null){status.setText("Estado: sin turno abierto");expected.setText("Monto esperado: —");diff.setText("Diferencia: —");}else{status.setText("Estado: turno abierto #"+a.id);expected.setText("Monto inicial: "+money(a.opening));diff.setText("Ventas en efectivo: "+money(cashSales(a.id)));}};
        open.addActionListener(e->{if(activeTurn()!=null){JOptionPane.showMessageDialog(frame,"Ya existe un turno abierto.");return;}try{double amount=Double.parseDouble(opening.getText().replace(',','.'));if(amount<0)throw new Exception();Turn t=new Turn();t.id=state.nextTurnId++;t.username=currentUser.username;t.start=LocalDateTime.now();t.opening=amount;t.open=true;state.turns.add(t);persist();fill.run();}catch(Exception ex){JOptionPane.showMessageDialog(frame,"Ingresa un monto inicial válido.");}});
        calc.addActionListener(e->{Turn t=activeTurn();if(t==null){JOptionPane.showMessageDialog(frame,"No hay turno abierto.");return;}double ex=t.opening+cashSales(t.id);expected.setText("Monto esperado: "+money(ex));});
        close.addActionListener(e->{Turn t=activeTurn();if(t==null){JOptionPane.showMessageDialog(frame,"No hay turno abierto.");return;}try{double ph=Double.parseDouble(physical.getText().replace(',','.'));if(ph<0)throw new Exception();t.expected=Math.round((t.opening+cashSales(t.id))*100.0)/100.0;t.physical=ph;t.difference=Math.round((ph-t.expected)*100.0)/100.0;t.end=LocalDateTime.now();t.open=false;persist();fill.run();JOptionPane.showMessageDialog(frame,"Turno cerrado.\nMonto esperado: "+money(t.expected)+"\nMonto físico: "+money(ph)+"\nDiferencia: "+money(t.difference));}catch(Exception ex){JOptionPane.showMessageDialog(frame,"Ingresa un efectivo físico válido.");}});
        fill.run();refresh();
    }
    private static double cashSales(int turnId){return state.sales.stream().filter(s->s.turnId==turnId&&s.paymentMethod.startsWith("Efectivo")).mapToDouble(s->s.total).sum();}

    private static void reports() {
        if(!isAdmin()){denied();return;}content.removeAll();content.setLayout(new BorderLayout(0,14));content.add(title("Reportes y análisis"),BorderLayout.NORTH);
        JPanel summary=new JPanel(new GridLayout(1,3,12,12));summary.setOpaque(false);double all=state.sales.stream().mapToDouble(s->s.total).sum();summary.add(metric("Ventas acumuladas",money(all),BLUE));summary.add(metric("Transacciones",String.valueOf(state.sales.size()),new Color(22,163,74)));summary.add(metric("Productos",String.valueOf(state.products.size()),new Color(147,51,234)));
        String[] cols={"Venta","Fecha","Cajero","Ítems","Método de pago","Total"};DefaultTableModel m=nonEditable(cols);JTable table=new JTable(m);styleTable(table);Runnable fill=()->{m.setRowCount(0);state.sales.stream().sorted((a,b)->b.date.compareTo(a.date)).forEach(s->m.addRow(new Object[]{"V-"+s.id,s.date.format(DT),s.username,s.items.size(),s.paymentMethod,money(s.total)}));};fill.run();JButton export=button("Exportar ventas CSV",new Color(22,163,74));export.addActionListener(e->exportSales());JPanel lower=new JPanel(new BorderLayout(0,8));lower.setOpaque(false);lower.add(new JScrollPane(table),BorderLayout.CENTER);lower.add(export,BorderLayout.SOUTH);content.add(summary,BorderLayout.CENTER);content.add(lower,BorderLayout.SOUTH);refresh();
    }
    private static void exportSales(){JFileChooser fc=new JFileChooser();fc.setSelectedFile(new File("tienda_agil_ventas.csv"));if(fc.showSaveDialog(frame)!=JFileChooser.APPROVE_OPTION)return;try(BufferedWriter w=Files.newBufferedWriter(fc.getSelectedFile().toPath(),StandardCharsets.UTF_8)){w.write("venta,fecha,cajero,metodo_pago,subtotal,igv,total\n");for(Sale s:state.sales)w.write(s.id+",\""+s.date.format(DT)+"\",\""+s.username+"\",\""+s.paymentMethod+"\","+s.subtotal+","+s.igv+","+s.total+"\n");JOptionPane.showMessageDialog(frame,"Reporte CSV exportado correctamente.");}catch(Exception ex){JOptionPane.showMessageDialog(frame,"No se pudo exportar: "+ex.getMessage());}}

    private static void settings() {
        if(!isAdmin()){denied();return;}content.removeAll();content.setLayout(new BorderLayout(0,14));content.add(title("Configuración"),BorderLayout.NORTH);
        JPanel body=new JPanel();body.setOpaque(false);body.setLayout(new BoxLayout(body,BoxLayout.Y_AXIS));JPanel gen=panelWithTitle("General",new JPanel(new GridLayout(0,2,8,8)));JPanel g=(JPanel)gen.getComponent(1);JTextField name=new JTextField(storeName);styleField(name);g.add(new JLabel("Nombre de la tienda"));g.add(name);JButton save=button("Guardar configuración",BLUE);g.add(new JLabel());g.add(save);save.addActionListener(e->{if(!name.getText().trim().isEmpty())storeName=name.getText().trim();frame.setTitle(storeName+" | Sistema de gestión");JOptionPane.showMessageDialog(frame,"Configuración aplicada durante esta sesión.");});body.add(gen);body.add(Box.createVerticalStrut(12));
        JPanel backupContent=new JPanel();backupContent.setBackground(Color.WHITE);backupContent.setLayout(new BoxLayout(backupContent,BoxLayout.Y_AXIS));backupContent.add(new JLabel("Respaldo local: copia el archivo de datos de la aplicación."));JButton backup=button("Crear backup local…",new Color(22,163,74));backup.setAlignmentX(Component.LEFT_ALIGNMENT);backupContent.add(Box.createVerticalStrut(10));backupContent.add(backup);backup.addActionListener(e->createBackup());backupContent.add(Box.createVerticalStrut(12));JLabel cloud=new JLabel("Cloud Backup: pendiente de configurar servicio, destino y credenciales.");cloud.setForeground(new Color(135,85,20));backupContent.add(cloud);body.add(panelWithTitle("Respaldo y sincronización",backupContent));content.add(body,BorderLayout.CENTER);refresh();
    }
    private static void createBackup(){JFileChooser fc=new JFileChooser();fc.setDialogTitle("Guardar copia local");fc.setSelectedFile(new File("tienda_agil_backup_"+LocalDate.now()+".bin"));if(fc.showSaveDialog(frame)!=JFileChooser.APPROVE_OPTION)return;try{persist();Files.copy(dataFile,fc.getSelectedFile().toPath(),StandardCopyOption.REPLACE_EXISTING);JOptionPane.showMessageDialog(frame,"Backup local creado correctamente.\nLa sincronización en la nube todavía requiere configuración.");}catch(Exception ex){JOptionPane.showMessageDialog(frame,"Error al crear el backup: "+ex.getMessage());}}

    private static void users() {
        if(!isAdmin()){denied();return;}content.removeAll();content.setLayout(new BorderLayout(0,14));content.add(title("Usuarios del sistema"),BorderLayout.NORTH);String[] cols={"Usuario","Nombre","Rol","Estado"};DefaultTableModel m=nonEditable(cols);JTable table=new JTable(m);styleTable(table);Runnable fill=()->{m.setRowCount(0);state.users.forEach(u->m.addRow(new Object[]{u.username,u.fullName,u.role,u.active?"Activo":"Inactivo"}));};fill.run();JButton add=button("Crear usuario",BLUE);add.addActionListener(e->{JTextField username=new JTextField(),full=new JTextField();JPasswordField pw=new JPasswordField();JComboBox<String> role=new JComboBox<>(new String[]{"CAJERO","ADMINISTRADOR"});JPanel p=new JPanel(new GridLayout(0,2,8,8));p.add(new JLabel("Usuario"));p.add(username);p.add(new JLabel("Nombre completo"));p.add(full);p.add(new JLabel("Contraseña inicial"));p.add(pw);p.add(new JLabel("Rol"));p.add(role);if(JOptionPane.showConfirmDialog(frame,p,"Nuevo usuario",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;String u=username.getText().trim(),n=full.getText().trim();char[] pass=pw.getPassword();if(u.isEmpty()||n.isEmpty()||pass.length<6){JOptionPane.showMessageDialog(frame,"Completa los campos y usa al menos 6 caracteres.");return;}if(state.users.stream().anyMatch(x->x.username.equalsIgnoreCase(u))){JOptionPane.showMessageDialog(frame,"Ese usuario ya existe.");return;}state.users.add(User.create(u,n,(String)role.getSelectedItem(),new String(pass)));Arrays.fill(pass,'\0');persist();fill.run();});JPanel wrapper=new JPanel(new BorderLayout(0,8));wrapper.setOpaque(false);wrapper.add(new JScrollPane(table),BorderLayout.CENTER);wrapper.add(add,BorderLayout.SOUTH);content.add(wrapper,BorderLayout.CENTER);refresh();
    }

    private static Turn activeTurn(){return state.turns.stream().filter(t->t.open&&t.username.equalsIgnoreCase(currentUser.username)).findFirst().orElse(null);}
    private static void logout(){if(frame!=null)frame.dispose();currentUser=null;cart.clear();showLogin();}
    private static void denied(){JOptionPane.showMessageDialog(frame,"Tu rol no tiene permisos para acceder a este módulo.","Acceso restringido",JOptionPane.WARNING_MESSAGE);}
    private static boolean sameDay(LocalDateTime dt,LocalDate day){return dt!=null&&dt.toLocalDate().equals(day);}
    private static String money(double x){return String.format(Locale.forLanguageTag("es-PE"),"S/ %.2f",x);}
    private static JLabel title(String text){JLabel l=new JLabel(text);l.setForeground(TEXT);l.setFont(new Font("SansSerif",Font.BOLD,25));return l;}
    private static JLabel label(String text){JLabel l=new JLabel(text);l.setForeground(TEXT);l.setFont(new Font("SansSerif",Font.BOLD,12));l.setBorder(new EmptyBorder(0,0,5,0));return l;}
    private static void styleField(JComponent c){
        c.setFont(new Font("SansSerif",Font.PLAIN,13));
        c.setForeground(TEXT);
        c.setBackground(Color.WHITE);
        if(c instanceof JTextComponent text){text.setCaretColor(TEXT);text.setSelectionColor(new Color(205,220,255));text.setSelectedTextColor(TEXT);}
        if(c instanceof JComboBox<?> combo){combo.setForeground(TEXT);combo.setBackground(Color.WHITE);}
        c.setPreferredSize(new Dimension(180,36));
        c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(213,219,228)),new EmptyBorder(7,9,7,9)));
    }
    private static JButton button(String text,Color color){
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setBackground(color);
        // Fuerza el pintado del fondo para evitar texto blanco sobre botones blancos
        // en distintos Look & Feel de Windows/NetBeans.
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setRolloverEnabled(false);
        b.setBorder(new EmptyBorder(10, 13, 10, 13));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private static JPanel metric(String name,String value,Color accent){JPanel p=new JPanel();p.setBackground(Color.WHITE);p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(224,228,235)),new EmptyBorder(14,15,14,15)));p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));JLabel n=new JLabel(name);n.setForeground(new Color(90,101,115));JLabel v=new JLabel(value);v.setForeground(accent);v.setFont(new Font("SansSerif",Font.BOLD,24));p.add(n);p.add(Box.createVerticalStrut(9));p.add(v);return p;}
    private static JPanel panelWithTitle(String heading,JComponent body){JPanel p=new JPanel(new BorderLayout(0,8));p.setBackground(Color.WHITE);p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(224,228,235)),new EmptyBorder(12,12,12,12)));JLabel title=new JLabel(heading);title.setFont(new Font("SansSerif",Font.BOLD,14));title.setForeground(TEXT);p.add(title,BorderLayout.NORTH);body.setOpaque(false);p.add(body,BorderLayout.CENTER);return p;}
    private static DefaultTableModel nonEditable(String[] cols){return new DefaultTableModel(cols,0){@Override public boolean isCellEditable(int r,int c){return false;}};}
    private static JPanel scrollTable(DefaultTableModel m){JPanel p=new JPanel(new BorderLayout());JTable t=new JTable(m);styleTable(t);p.add(new JScrollPane(t));return p;}
    private static void styleTable(JTable t){
        t.setRowHeight(28);
        t.setFont(new Font("SansSerif",Font.PLAIN,12));
        t.setForeground(TEXT);
        t.setBackground(Color.WHITE);
        t.setSelectionForeground(Color.WHITE);
        t.setSelectionBackground(BLUE);
        t.setGridColor(new Color(230,233,238));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setFillsViewportHeight(true);
        t.getTableHeader().setFont(new Font("SansSerif",Font.BOLD,12));
        t.getTableHeader().setForeground(TEXT);
        t.getTableHeader().setBackground(new Color(235,238,243));
        t.getTableHeader().setOpaque(true);
    }
    private static void refresh(){if(content!=null){content.revalidate();content.repaint();}}

    // Password hashing with PBKDF2-HMAC-SHA256; a hash is stored, not the plain password.
    private static byte[] hashPassword(char[] password, byte[] salt) {
        try { PBEKeySpec spec=new PBEKeySpec(password,salt,120_000,256);byte[] result=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();spec.clearPassword();return result; }
        catch(Exception ex){throw new IllegalStateException("No se pudo proteger la contraseña",ex);}
    }

    public static class AppState implements Serializable {
        private static final long serialVersionUID=1L;
        List<User> users=new ArrayList<>(); List<Product> products=new ArrayList<>(); List<Sale> sales=new ArrayList<>(); List<Turn> turns=new ArrayList<>();
        int nextProductId=1,nextSaleId=1,nextTurnId=1;
    }
    static class User implements Serializable {
        private static final long serialVersionUID=1L;String username,fullName,role;byte[] salt,passwordHash;boolean active=true;
        static User create(String u,String n,String r,String password){User x=new User();x.username=u;x.fullName=n;x.role=r;x.salt=new byte[16];new SecureRandom().nextBytes(x.salt);x.passwordHash=hashPassword(password.toCharArray(),x.salt);return x;}
        boolean matches(char[] password){return MessageDigest.isEqual(passwordHash,hashPassword(password,salt));}
    }
    static class Product implements Serializable {
        private static final long serialVersionUID=1L;int id;String barcode,name,category;double price;int stock,minStock;boolean active;
        Product(int id,String barcode,String name,String category,double price,int stock,int min,boolean active){this.id=id;this.barcode=barcode;this.name=name;this.category=category;this.price=price;this.stock=stock;this.minStock=min;this.active=active;}
        Product copy(){return new Product(id,barcode,name,category,price,stock,minStock,active);}
    }
    static class CartLine {Product product;int quantity;CartLine(Product p,int q){product=p;quantity=q;}}
    static class Sale implements Serializable {
        private static final long serialVersionUID=1L;int id,turnId;LocalDateTime date;String username,paymentMethod;double subtotal,igv,total,received,change;List<SaleItem> items=new ArrayList<>();
    }
    static class SaleItem implements Serializable {
        private static final long serialVersionUID=1L;int productId,quantity;String name;double price,subtotal;
        SaleItem(int id,String name,int qty,double price,double sub){productId=id;this.name=name;quantity=qty;this.price=price;subtotal=sub;}
    }
    static class Turn implements Serializable {
        private static final long serialVersionUID=1L;int id;String username;LocalDateTime start,end;double opening,expected;Double physical,difference;boolean open;
    }
}
