package oma.koodi.ylis;

import java.awt.event.ActionListener;
import java.io.BufferedReader;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;
import java.text.ParseException;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;
 
/**
 *
 * @author inka ratia
 *  
 */


public class Ylis extends Application implements ActionListener {
    
    // luodaan syötetiedostolle File-tiedosto, ja posts, replies & del_posts
    //multimapit säilömään postausten, vastausten ja poistettujen/muualla olevien
    // postausten dataa.
    File file;
    static MultiMap posts = new MultiValueMap();
    static MultiMap replies = new MultiValueMap();
    static MultiMap del_posts = new MultiValueMap();
    
    // luodaan workbook-objekti ulostulolle.
    static XSSFWorkbook workbook = new XSSFWorkbook();
        
    public static void parseFile(File file) throws FileNotFoundException, IOException {
        
        // alustetaan käyttäjän id, postausid, postaajan numero ja poistettujen
        //vastausten postausid.
        String userid = null;
        String postid = null;
        String userpostnum = null;     
        String del_response = null;
        // kaava integerien nappaamiseen lähdekoodista.
        Pattern just_integers = Pattern.compile("\\d+");
        
        // luetaan tiedostoa rivi kerrallaan, jos löydetään "data-user-id"
        // tallennetaan sen arvo ja postaajan numero posts-säiliöön.
        try(BufferedReader input = new BufferedReader(new FileReader(file))) {
            String line = null;
            while ((line = input.readLine()) != null ) {          
                if (line.contains("data-user-id")) { 
                    userid = line;
                    int user_index = userid.indexOf("attribute-value"); 
                    userid = userid.substring(user_index);                   
                    Matcher find_user_integers = just_integers.matcher(userid);
                    while (find_user_integers.find()) {
                      //  int post_index = postid.indexOf("data-post-id");
                        int post_index = postid.indexOf("attribute-value");
                        userpostnum = postid.substring(post_index);
                        Matcher find_post_integers = just_integers.matcher(userpostnum);
                        while (find_post_integers.find()) {
                            int post_integers = Integer.parseInt(find_post_integers.group());
                            posts.put(find_user_integers.group(), post_integers);
                        }
                    }   
                }
                
                // jos rivillä on vastaus, tallennetaan postaajan ja vastauksen
                // kohteen postausnumerot replies-säiliöön.
                else if (line.contains("data-replies")) {   
                    String repliedTopostid = null;
                    String replierpostid = null;
                    int reply_index = line.indexOf("data-replies");
                    // alustettu metodin alussa, vastatun postauksen ID.
                    repliedTopostid = line.substring(0, reply_index);
                    int line_index = repliedTopostid.indexOf("data-post-id");
                    repliedTopostid = repliedTopostid.substring(line_index);
                    // alustettu metodin alussa, postaukseen vastaajien ID.
                    replierpostid = line.substring(reply_index);
                    Matcher find_repliedTo_id = just_integers.matcher(repliedTopostid);
                    Matcher find_replier_id = just_integers.matcher(replierpostid);
                    while (find_repliedTo_id.find()) {                        
                        while (find_replier_id.find()) {
                            int repliedTo_id = Integer.parseInt(find_repliedTo_id.group());
                            int replier_id = Integer.parseInt(find_replier_id.group());
                            replies.put(repliedTo_id, replier_id);                           
                        }
                    }  
                }
                // tallennetaan kaikki maininnat poistettuihin postauksiin.
                else if (line.contains("post-button icon-menu")) {
                    int id_index_start = line.lastIndexOf("attribute-value");
                    del_response = line.substring(id_index_start);
                    Matcher find_del_response = just_integers.matcher(del_response);
                    if (find_del_response.find()) {
                        del_response = find_del_response.group();
                    }
                }
                
                else if (line.contains("post-message")) {
                    int rogue_index_start = line.indexOf("â");
                    int rogue_index_end = line.indexOf("end-tag");
                    
                    if (line.contains("â") && line.contains("end-tag")) {
                        String deleted_post = line.substring(rogue_index_start,rogue_index_end);
                        Matcher find_rogue_id = just_integers.matcher(deleted_post);
                        
                        if (find_rogue_id.find()) {
                            int rogue_id = Integer.parseInt(find_rogue_id.group());
                            int del_response_id = Integer.parseInt(del_response);
                            del_posts.put(del_response_id, rogue_id);                     
                        }                     
                    }
                }
                else {
                    postid = line;
                }
            }        
        }
        // jos poistettuihin postauksiin lisätty postaus löytyy jo aiemmin 
        // täytetystä posts-säiliöstä, poistetaan se del_posts-säiliöstä.
        // jäljelle jää vain poistetut / muualla (toisessa ketjussa) vastatut.
        Collection del_keys = (Collection) del_posts.keySet();
        Iterator del_it_keys = del_keys.iterator();
        boolean deleted = false;
        while (del_it_keys.hasNext()) {
            Collection del_values = (Collection) del_posts.get(del_it_keys.next());
            Iterator del_it_values = del_values.iterator();
            while (del_it_values.hasNext()) {
                if (posts.containsValue(del_it_values.next())) {
                    del_posts.remove(del_it_keys.toString());
                    del_it_values.remove();     
                    deleted = true;
                }
            }
            if (deleted) {
                del_it_keys.remove();
                deleted = false;
            }
        }
        // siirrytään lisäämään säiliöiden tiedot workbook-objektiin.
        parseIds();
    }
    
    /**
     *
     * @param data
     * @return
     * etsitään posts-säiliöstä korreloivan postausnumeron avulla postaajan
     * numero ja palautetaan se. jos ei löydy annetaan arvo 99999 (ei mahdollinen
     * sivuston antamissa numeroissa, toimittaa nullin virkaa).
     */
    public static int findUserId(Object data) {
        
        int userid = 99999;
         for ( Object post : posts.keySet()) {
            Collection allposts = (Collection) posts.get(post);
            if (allposts.contains(data)) {
                userid = Integer.parseInt((String) post);
            }
         }
        return userid;
    }
    public static void parseIds() throws IOException {
        
        // luo uuden sporeadsheet-objektin, eli
        // uuden taulukon workbook-objektiin.

        XSSFSheet spreadsheet = workbook.createSheet("Ylis API Data");
        // luodaan rivi
        XSSFRow row;
        int rowid = 0;
        
        /* tallennetaan excelin eka sarake eli vastanneet
         * haetaan postaajan numero findUserId-funktiosta.
         */
        // iteroitaan kaikkien postausten läpi. 
        for (Object post_it : posts.values()) {
            // jos postaus löytyy myös vastauksista etsitään vastauksen kohde,
            // ja lisätään taulukkoon.
            if (replies.containsKey(post_it)) { 
                if (!replies.containsValue(post_it)) {
                    row = spreadsheet.createRow(rowid);
                    Cell firstcell = row.createCell(0);
                    Cell extracell = row.createCell(1);
                    Cell secondcell = row.createCell(2);
                    firstcell.setCellValue(findUserId(post_it));
                    extracell.setCellValue((int)post_it);
                    secondcell.setCellValue("#N/A");
                    rowid++;
                }
                    Collection values = (Collection) replies.get(post_it);
                    Iterator valuesIterator = values.iterator( );
                    while( valuesIterator.hasNext() ) {
                        row = spreadsheet.createRow(rowid);
                        Cell firstcell = row.createCell(0);
                        Cell extracell = row.createCell(1);
                        Cell secondcell = row.createCell(2);
                        Object v = (int)valuesIterator.next();
                        
                        firstcell.setCellValue(findUserId(v));
                        extracell.setCellValue((int)v);
                        secondcell.setCellValue(findUserId(post_it));
                        rowid++;  
                    }                 
            }              
            else {
                // jos postaus EI löydy vastatuista (replies-säiliö) eikä
                // poistetuista (del_posts) lisätään se postauksena jolla
                // ei ole vastattua (replied_to) viestiä, vaan "#N/A" arvo.
                if (!replies.containsValue(post_it) && (!del_posts.containsKey(post_it))) {
                    // postaus ei ole vastauksena kellekkään, etsitään postaajan arvo ja lisätään sellaisenaan.
                    row = spreadsheet.createRow(rowid);
                    Cell firstcell = row.createCell(0);
                    Cell extracell = row.createCell(1);
                    Cell secondcell = row.createCell(2);
                    firstcell.setCellValue(findUserId(post_it));
                    extracell.setCellValue((int)post_it);
                    secondcell.setCellValue("#N/A");
                    rowid++;
                }
                
                // jos postaus löytyy poistetuista, luodaan sille oma rivi
                // ja poistetaan se sitten säiliöstä duplikaattien estämiseksi.
                else if (del_posts.containsKey(post_it)) {
                    row = spreadsheet.createRow(rowid);
                    Cell firstcell = row.createCell(0);
                    Cell extracell = row.createCell(1);
                    Cell secondcell = row.createCell(2);
                    firstcell.setCellValue(findUserId(post_it));
                    extracell.setCellValue((int)post_it);
                    secondcell.setCellValue(findUserId(del_posts.get(post_it)));
                    del_posts.remove(post_it);
                    rowid++;
                }
            }             
        }         
    }
        
    @Override
    public void start(Stage stage) throws ParseException, IOException {
        
        // luodaan käyttöliittymä ohjelmalle.
        GridPane dialogWindow = new GridPane();
        Scene scene = new Scene(dialogWindow, 500, 750);             
        stage.setTitle("Yliskoodi");
        
        FileChooser filechooser = new FileChooser();
        Label label = new Label("Hae & lataa htm-tyyppinen tiedosto langan view-source -sivulta.");
        Label fieldHttp = new Label();
        fieldHttp.setText("esim.          https _ylilauta.org_aihevapaa_129491954.htm");
        fieldHttp.setStyle("-fx-prompt-text-fill: derive(-fx-control-inner-background,-30%); }");;
        
        // nappeja valinnalle, tallennukselle ja uudelle ikkunalle.
        Button chooseButton = new Button("Valitse tiedosto");
        Button saveButton = new Button("Tallenna tiedosto");
        Button closeButton = new Button ("Uudestaan (avaa uuden ikkunan).");
        Button postlistButton = new Button("")
        saveButton.setDisable(true);
        Label labelAnswer = new Label("");
        
        // jos käyttäjä painaa valintanäppäintä avataan tiedoston valitsin.
        chooseButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                file = filechooser.showOpenDialog(stage);
                
                try {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    // kun valinta onnistuu näkyy näytöllä "valittu + tiedoston_sijainti"
                    labelAnswer.setText("Valittu:  " + file.getAbsolutePath());
                    saveButton.setDisable(false);
                    label.setDisable(true);
                    fieldHttp.setDisable(true);
                    chooseButton.setDisable(true);
                    parseFile(file);
                    
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(Ylis.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(Ylis.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        // tallennusnäppäin. avaa filechooser-dialogin ja antaa ehdotetun nimen
        // tiedostolle.
        saveButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) { 
                FileChooser filesaver = new FileChooser();
                filesaver.setTitle("Save");
                FileChooser.ExtensionFilter filter = 
                new FileChooser.ExtensionFilter("XLSX files (*.xlsx)", "*xlsx");
                filesaver.getExtensionFilters().add(filter);
                String filename = file.getName();
                int startindex = filename.indexOf("ylilauta.org_");
                int endindex = filename.indexOf(".htm");
                filename = filename.substring(startindex, endindex);
                filesaver.setInitialFileName(filename + ".xlsx");
                File savefile = filesaver.showSaveDialog(stage);
                
                if (file != null) {
                    try (FileOutputStream outputStream = new FileOutputStream(savefile.getAbsolutePath())) {
                        workbook.write(outputStream);
                    }
                    catch (IOException ex) {
                        Logger.getLogger(Ylis.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                
                
                                  
            }
        });
        
        closeButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {       
                Stage stage = new Stage();
                try {
                    start(stage);
                } catch (ParseException ex) {
                    Logger.getLogger(Ylis.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(Ylis.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        
        //FileOutputStream out = null;
        dialogWindow.add(label, 0, 0);
        dialogWindow.add(fieldHttp, 0, 2);
        dialogWindow.add(chooseButton, 0, 3);
        dialogWindow.add(labelAnswer, 0, 4);
        dialogWindow.add(saveButton, 0, 5);
        dialogWindow.add(closeButton, 0, 7);
        dialogWindow.setHgap(10);
        dialogWindow.setVgap(10);
        dialogWindow.setPadding(new Insets(10,10,10,10));
        stage.setScene( scene );
        
        stage.show(); 
    }            
 
    public static void main(String[] args) throws FileNotFoundException, IOException
    { 
        launch();
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
//