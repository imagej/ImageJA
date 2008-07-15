import java.sql.*;
import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
//import ij.plugin.frame.*;

/* This is an example how to link ImageJ to a RDBMS engine like MySQL
 * you should change the database, dbHost, dbTable, dbUser and dbPass
 * to match your system
 *
 *     @author	Dimiter Prodanov
 *     @author  University of Leiden
 *
 *      Copyright (C) 2003 Dimiter Prodanov
 *
 *      This library is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *       Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public
 *      License along with this library; if not, write to the Free Software
 *      Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *
 */

public class MySQL_PlugIn implements PlugIn  {
    
    private final static String driver  = "com.mysql.jdbc.Driver";
    
    private String database  = "darea";
    private String dbHost  = "localhost";
    private String dbTable = "config";
    private String dbUser="web";
    private String dbPass="";
    private String dbUrl="jdbc:mysql://"+ dbHost+ "/" + database;
    
    private boolean connected=false;
    
    // the DataBase Connection
    private Connection con;
    
    public void run(String arg) {
        ConnStart(database);
        ConnDestroy();
    }
    
    
    public void ConnStart(String dbName) {
        try {
            //Class.forName("org.gjt.mm.mysql.Driver");
            Class.forName(driver);
        } catch(Exception ex) {
            IJ.log("Can't find Database driver class: " + ex);
            return;
        }
        
        
        
        try {
            con = (Connection) DriverManager.getConnection(dbUrl, dbUser, dbPass);
            IJ.log("Connected to " + dbUrl);
            connected=true;
        } catch(SQLException ex) {
            IJ.log("SQLException: " + ex);
        }
    }
    
    public void ConnDestroy() {
        if (connected) {
            try {
                con.close();
                IJ.log("Disconnected from database");
            } catch(SQLException ex) {
                IJ.log("SQLException: " + ex);
            }
        }
    }
    
}
