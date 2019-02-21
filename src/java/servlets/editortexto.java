/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servlets;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.RequestDispatcher;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import javax.xml.bind.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Gerardo
 */
/* Localizacion por defecto
 Location = C:\Usuarios\..usuario..\AppData\Roaming\NetBeans\8.0.2\config\GF_4.1\domain1\generated\jsp\EditorTextoJSP
*/

@MultipartConfig(location = "C:\\Users\\Gerardo\\Documents\\NetBeansProjects\\EditorTextoJSP\\build\\web\\archivos",
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 1024 * 1024 * 5,
        maxRequestSize = 1024 * 1024 * 5 * 5)

public class editortexto extends HttpServlet {

    private String ruta; //ruta de los archivos de usuario
    private Properties usuarios; //coleccion de usuarios del ini
    private final String ficheroUsuarios = "usuarios.ini"; //fichero de usuarios
    ArrayList<String> historial; //coleccion que almacena el historial de los archivos abiertos y cerrados

    @Override
    /**
     * Se llama una sola vez, con la primera peticion
     * Ideal para inicializar los recursos que se vayan a utilizar
     * por parte de todos los usuarios a lo largo de toda la vida
     * de la aplicacion
     */
    public void init() {
        //creamos la coleccion para el historial
        historial = new ArrayList<>();
        //lo guardamos a nivel de aplicacion para que este disponible para todas las paginas
        this.getServletContext().setAttribute("historial", historial);
        ruta = this.getServletContext().getRealPath("/") + "archivos" + File.separator;
        usuarios = new Properties();
        try {
            usuarios.load(new FileReader(ruta + ficheroUsuarios));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(editortexto.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(editortexto.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession sesion;
        String url = null;
        String texto = null;
        String archivo = null;
        boolean ok;
        
        //Recojo la sesion para almacenar mensajes en su mayor parte
        sesion = request.getSession();
        String accion = request.getParameter("accion");
        accion = accion.toLowerCase().replaceAll(" ", "");
        switch (accion) {
            case "abrir":
                archivo = request.getParameter("archivo");
                try {
                    texto = abrir(archivo);
                    sesion.setAttribute("archivoActual", archivo);
                } catch (FileNotFoundException ex) {
                    sesion = request.getSession();
                    sesion.setAttribute("error", "No se encuentra el archivo");
                }
                url = "index.jsp?texto=" + texto;
                break;
            case "guardar":
                archivo = request.getParameter("archivo");
                texto = request.getParameter("texto");
                guardar(texto, archivo);
                sesion.setAttribute("archivoActual", archivo);
                url = "index.jsp";
                break;
            case "cargar":
                archivo = request.getParameter("historial");
                try {
                    texto = abrir(archivo);
                    url = "index.jsp?texto=" + texto;
                    sesion.setAttribute("archivoActual", archivo);
                } catch (FileNotFoundException ex) {
                    sesion = request.getSession();
                    sesion.setAttribute("error", "No se encuentra el archivo");
                    url = "index.jsp";
                }

                break;
            case "acceder":
                ok = acceder(request);
                if (ok) {
                    url = "index.jsp";
                    sesion.setAttribute("usuario", request.getParameter("login"));
                    sesion.setAttribute("archivoActual", "nuevo.txt");
                } else {
                    // Preparamos el mensaje a través de la sesión
                    sesion.setAttribute("error", "Usuario incorrecto");
                    url = "acceso.jsp";
                }
                break;
            case "registrar":
                ok = registrar(request);
                if (ok) {
                    sesion.setAttribute("error", "El usuario se ha registrado correctamente");
                } else {
                    sesion.setAttribute("error", "Ya existe un usuario con el mimso nombre");
                }
                url = "acceso.jsp";
                break;
            case "buscar":
                url = "buscar.jsp";
                break;
            case "imprimir":
                imprimir(request, response);
                break;
            case "subir":
                ok = upload(request);
                url = "index.jsp";
                if (!ok) {
                    sesion.setAttribute("error", "No ha sido posible subir el archivo");
                }
                break;
            case "guardarcomopdf":
                url = "index.jsp";
                guardarComoPdf(request, response);
                break;
            case "crearcarpeta":
                crearCarpeta(request, response);
                url = "index.jsp";
                break;
            case "importarcomoxml":
                ok = importarcomoxml(request, response);
                url = "acceso.jsp";
                if (!ok) {
                    sesion.setAttribute("error", "No ha sido posible realizar la importación");
                }
                break;
            case "exportarcomoxml":
                exportarcomoXml(request, response);
                return;

        }

        if (url != null) {
            // Preparamos el tipo de respuesta
            // response.setContentType("text/html;charset=UTF-8");
            // Redireccionamos a la url que corresnponda
            //  response.sendRedirect(url);
            RequestDispatcher rd = getServletContext().getRequestDispatcher("/" + url);
            rd.forward(request, response);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

    private String abrir(String archivo) throws IOException {

        BufferedReader fichero;
        fichero = new BufferedReader(new FileReader(ruta + archivo));
        String texto = fichero.readLine();
        while (fichero.ready()) {
            texto = texto + fichero.readLine();
        }
        fichero.close();
        historial.add(archivo);
        return texto;
    }

    private void guardar(String texto, String archivo)
            throws FileNotFoundException {

        PrintWriter fichero;
        fichero = new PrintWriter(ruta + archivo);
        fichero.write(texto);
        fichero.flush();
        fichero.close();
        historial.add(archivo);
    }

    private boolean acceder(HttpServletRequest request) {

        boolean ok = true;
        String login, password, clave;

        login = request.getParameter("login");
        password = request.getParameter("password");
        clave = usuarios.getProperty(login);
        if (clave == null || !clave.equals(password)) {
            ok = false;
        }
        return ok;
    }

    private boolean registrar(HttpServletRequest request) throws IOException {

        boolean ok = false;
        String login, password, clave;

        login = request.getParameter("login");
        password = request.getParameter("password");
        clave = usuarios.getProperty(login);
        if (clave == null) {
            usuarios.setProperty(login, password);
            usuarios.store(new FileWriter(ruta + ficheroUsuarios), "");
            usuarios.storeToXML(new FileOutputStream(ruta + ficheroUsuarios + ".xml"), "");
            ok = true;
        }
        return ok;
    }

    private boolean imprimir(HttpServletRequest request, HttpServletResponse response) {

        boolean ok = true;
        String nombreArchivo;

        nombreArchivo = (String) request.getSession().getAttribute("archivoActual") + ".pdf";
        // Establecemos el tipo de respuesta
        response.setContentType("application/pdf");
        response.setHeader("Content-disposition", "attachment;filename=" + nombreArchivo);
        /* Creamos un objeto documento con dimensiones A4 sobre 
        el que iremos añadiendo el resto de elementos */
        Document document = new Document(PageSize.A4);
        // Creamos un buffer temporal 
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            /* Creamos un escritor que interactuará con el 
            documento a través del buffer. 
            El buffer es el destino, o sea, que para grabarlo en un fichero
            debería ser un objeto FileOutputStream */
            PdfWriter.getInstance(document, buffer);
            generarPDF(document, request);
            // Enviamos el documento como bytes por la salida de la respuesta
            DataOutput output = new DataOutputStream(response.getOutputStream());
            byte[] bytes = buffer.toByteArray();
            response.setContentLength(bytes.length);
            for (int i = 0; i < bytes.length; i++) {
                output.writeByte(bytes[i]);
            }

        } catch (DocumentException | IOException ex) {
            Logger.getLogger(editortexto.class.getName()).log(Level.SEVERE, null, ex);
            ok = false;
        }

        return ok;
    }

    private PdfPCell crearCelda(String texto, BaseColor color) {

        Paragraph parrafo = new Paragraph();
        parrafo.setFont(new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, color));
        // Nombre del archivo
        parrafo.add(texto);
        parrafo.setAlignment(Paragraph.ALIGN_CENTER);
        parrafo.setSpacingAfter(4);
        // Creamos una celda
        PdfPCell celda = new PdfPCell(parrafo);
        celda.setHorizontalAlignment(PdfPCell.ALIGN_JUSTIFIED);
        celda.setVerticalAlignment(PdfPCell.ALIGN_CENTER);
        // Añadimos el párafo a la celda
        celda.addElement(parrafo);
        return celda;
    }

    private boolean upload(HttpServletRequest request) throws ServletException {

        boolean ok = true;

        HttpSession sesion = request.getSession();
        try {
            Part fichero = request.getPart("fichero");
            fichero.write(fichero.getSubmittedFileName());
            sesion.setAttribute("error", "Nombre:" + fichero.getName() + "Fichero:" + fichero.getSubmittedFileName());
        } catch (IOException ex) {
            ok = false;
            sesion.setAttribute("error", ex.getMessage());
        }
        return ok;
    }

    private boolean guardarComoPdf(HttpServletRequest request, HttpServletResponse response) {

        boolean ok = true;
        String path;

        path = ruta + request.getParameter("archivo") + ".pdf";
        // Establecemos el tipo de respuesta
        response.setContentType("application/pdf");
        /* Creamos un objeto documento con dimensiones A4 sobre 
        el que iremos añadiendo el resto de elementos */
        Document document = new Document(PageSize.A4);

        try {
            /* Creamos un escritor que interactuará con el
            documento a través del buffer.
            El buffer es el destino, o sea, que para grabarlo en un fichero
            debería ser un objeto FileOutputStream */
            PdfWriter.getInstance(document, new FileOutputStream(path));
            generarPDF(document, request);
        } catch (FileNotFoundException | DocumentException ex) {
            Logger.getLogger(editortexto.class.getName()).log(Level.SEVERE, null, ex);
            ok = false;
        }
        return ok;
    }

    private void generarPDF(Document document, HttpServletRequest request) throws DocumentException {

        // Abrimos el documento
        document.open();

        // Creamos una tabla de dos columnas iguales
        PdfPTable tabla = new PdfPTable(3);
        tabla.setTotalWidth(new float[]{90, 90, 90});
        tabla.setHorizontalAlignment(PdfPTable.ALIGN_CENTER);

        // LA CABECERA DE LA TABLA
        Paragraph parrafo = new Paragraph();
        parrafo.setFont(new Font(FontFamily.HELVETICA, 14, Font.BOLD, new BaseColor(0, 0, 255)));
        parrafo.setAlignment(Paragraph.ALIGN_CENTER);
        parrafo.add("Ejemplo de generación de un documento PDF");
        parrafo.setSpacingAfter(8);
        // Creamos una celda
        PdfPCell celda = new PdfPCell(parrafo);
        celda.setColspan(3);
        celda.setHorizontalAlignment(PdfPCell.ALIGN_JUSTIFIED);
        celda.setVerticalAlignment(PdfPCell.ALIGN_CENTER);
        // Añadimos el párafo a la celda
        celda.addElement(parrafo);
        // Añadimos la  celda a la tabla
        tabla.addCell(celda);

        // Añadimos las cabeceras de las columnas 
        celda = crearCelda("Autor", new BaseColor(255, 0, 0));
        tabla.addCell(celda);

        celda = crearCelda("Fecha", new BaseColor(255, 0, 0));
        tabla.addCell(celda);

        celda = crearCelda("Archivo", new BaseColor(255, 0, 0));
        tabla.addCell(celda);

        // Añadimos la información relativa a cada cabecera
        HttpSession sesion = request.getSession();
        String nombreUsuario = (String) sesion.getAttribute("usuario");
        celda = crearCelda(nombreUsuario, new BaseColor(0, 255, 0));
        tabla.addCell(celda);

        Date fecha = new Date();
        celda = crearCelda(fecha.toString(), new BaseColor(0, 255, 0));
        tabla.addCell(celda);

        String nombreArchivo = (String) sesion.getAttribute("archivoActual");
        celda = crearCelda(nombreArchivo, new BaseColor(0, 255, 0));
        tabla.addCell(celda);

        // Finalmente el archivo de texto
        String texto = request.getParameter("texto");
        parrafo = new Paragraph();
        parrafo.setFont(new Font(FontFamily.HELVETICA, 14, Font.NORMAL));
        parrafo.setAlignment(Paragraph.ALIGN_JUSTIFIED);
        parrafo.setSpacingAfter(2);
        parrafo.add(texto);
        // Creamos una celda
        celda = new PdfPCell(parrafo);
        celda.setColspan(3);
        celda.setHorizontalAlignment(PdfPCell.ALIGN_JUSTIFIED);
        celda.setVerticalAlignment(PdfPCell.ALIGN_CENTER);
        // Añadimos el párafo a la celda
        celda.addElement(parrafo);
        // Añadimos la  celda a la tabla
        tabla.addCell(celda);

        // Agregamos la tabla al documento
        document.add(tabla);

        // Cerramos el documento
        document.close();

    }

    private boolean crearCarpeta(HttpServletRequest request, HttpServletResponse response) {

        boolean ok;
        String carpeta;

        carpeta = request.getParameter("archivo");
        File directorio = new File(ruta + carpeta);
        ok = directorio.mkdir();
        return ok;
    }

    private boolean importarcomoxml(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        boolean ok = true;

        try {
            PrintWriter out = response.getWriter();
            Part fichero = request.getPart("fichero");

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
            org.w3c.dom.Document document = documentBuilder.parse(fichero.getInputStream());

            document.getDocumentElement().normalize();
            //out.println("Elemento raiz:" + document.getDocumentElement().getNodeName());
            NodeList listaUsuarios = document.getElementsByTagName("usuario");
            for (int temp = 0; temp < listaUsuarios.getLength(); temp++) {
                Node nodo = listaUsuarios.item(temp);
                out.println("Elemento:" + nodo.getNodeName());
                if (nodo.getNodeType() == Node.ELEMENT_NODE) {
                    org.w3c.dom.Element element = (org.w3c.dom.Element) nodo;
                    //out.println("id: " + element.getAttribute("id"));
                    //out.println("Nombre: " + element.getElementsByTagName("nombre").item(0).getTextContent());
                    //out.println("username: " + element.getElementsByTagName("username").item(0).getTextContent());
                    //out.println("password: " + element.getElementsByTagName("password").item(0).getTextContent());
                    String username = element.getElementsByTagName("username").item(0).getTextContent();
                    String password = element.getElementsByTagName("password").item(0).getTextContent();
                    usuarios.setProperty(username, password);
                }
            }
            usuarios.store(new FileWriter(ruta + ficheroUsuarios), "");
        } catch (SAXException | ParserConfigurationException ex) {
            Logger.getLogger(editortexto.class.getName()).log(Level.SEVERE, null, ex);
            ok = false;
        }

        return ok;
    }

    private void exportarcomoXml(HttpServletRequest request, HttpServletResponse response) {

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // elemento raiz
            org.w3c.dom.Document doc = docBuilder.newDocument();
            org.w3c.dom.Element rootElement = doc.createElement("usuarios");
            doc.appendChild(rootElement);
            Enumeration listaUsuarios = usuarios.propertyNames();
            int i = 0;
            while (listaUsuarios.hasMoreElements()) {
                String propiedad = (String) listaUsuarios.nextElement();
                String clave = usuarios.getProperty(propiedad);

                // usuario
                org.w3c.dom.Element usuario = doc.createElement("usuario");
                rootElement.appendChild(usuario);

                // atributo del elemento empleado
                Attr attr = doc.createAttribute("id");
                attr.setValue(String.valueOf(i));
                usuario.setAttributeNode(attr);

                // login
                org.w3c.dom.Element login = doc.createElement("login");
                login.appendChild(doc.createTextNode(propiedad));
                usuario.appendChild(login);

                // password
                org.w3c.dom.Element password = doc.createElement("password");
                password.appendChild(doc.createTextNode(clave));
                usuario.appendChild(password);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            // StreamResult result = new StreamResult(new File("C:\\archivo.xml"));
            // Si se quiere mostrar por la consola...
            // StreamResult result = new StreamResult(System.out);
            response.setContentType("text/xml");
            response.setHeader("Content-disposition","attachment");
            StreamResult result = new StreamResult(response.getOutputStream());

            
            transformer.transform(source, result);

        } catch (ParserConfigurationException | TransformerException | IOException ex) {
            Logger.getLogger(editortexto.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
