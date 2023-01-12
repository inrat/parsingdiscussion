
package oma.koodi.ylis;

/**
 *
 * @author inka ratia
 *  Ohjelma: StartYlis, käynnistää ohjelman Ylis.java.
 * 
 * 19.9.2022
 * 
 * Ohjelma käynnistää käyttöliittymän, joka pyytää syötteenä .htm-tyyppistä
 * tiedostoa, joka on haettu view-source -sivulta. Vaatii ennakkotietoa tiedoston
 * hausta, ja olettaa että tiedosto on ylilauta.org -sivuston lähdekoodia, joka
 * on saman tyyppistä kuin luontipäivänä. Jos ei toimi halutulla tavalla täytyy
 * tehdä muutoksia.
 * 
 * Ottaa tiedoston, parsii sen läpi ja kerää postaustiedot säiliöihin. Purkaa
 * säiliöt workbook-tiedostoon ja antaa sen käyttäjälle ladattavaksi. 
 * Uusimisnappi avaa ohjelmasta uuden ikkunan.
 */

public class StartYlis {

    public static void main(final String args[]) throws Exception {
        Ylis.main(args);
    }
}
