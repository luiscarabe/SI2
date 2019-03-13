/**
 * Pr&aacute;ctricas de Sistemas Inform&aacute;ticos II
 * VisaCancelacionJMSBean.java
 */

package ssii2.visa;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.ejb.ActivationConfigProperty;
import javax.jms.MessageListener;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.JMSException;
import javax.annotation.Resource;
import java.util.logging.Logger;

/**
 * @author jaime
 */
@MessageDriven(mappedName = "jms/VisaPagosQueue")
public class VisaCancelacionJMSBean extends DBTester implements MessageListener {
  static final Logger logger = Logger.getLogger("VisaCancelacionJMSBean");
  @Resource
  private MessageDrivenContext mdc;

  // Definir UPDATE sobre la tabla pagos para poner
  // codRespuesta a 999 dado un código de autorización
  private static final String UPDATE_CANCELA_QRY =
      "update pago" +
      " set codrespuesta=999" +
      " where idautorizacion=?";

    private static final String RECTIFICAR_SALDO_QRY =
    "update tarjeta" +
    " set saldo = saldo + pago.importe" +
    " from pago"+
    " where pago.idautorizacion=?"+
    " and tarjeta.numerotarjeta = pago.numerotarjeta";

    private static final String SELECT_CODRESPUESTA_QRY =
                    "select codrespuesta " +
                    " from pago " +
                    " where idautorizacion = ?";


  public VisaCancelacionJMSBean() {
  }

  private boolean ejecutarConsultaActualizacion(String consulta, int idAutorizacion) {
    Connection con = null;
    boolean exito = false;
    try {
    con = getConnection();

    PreparedStatement pstmt = con.prepareStatement(consulta);

    pstmt.setInt(1, idAutorizacion);
    exito = !pstmt.execute() && pstmt.getUpdateCount() == 1;
    if (!exito) {
      logger.warning("Ha ocurrido un error al ejecutar la consulta");
    }
    pstmt.close();
    }catch (SQLException e) {
      logger.warning(e.getMessage());
    }
    return exito;
  }

  // TODO : Método onMessage de ejemplo
  // Modificarlo para ejecutar el UPDATE definido más arriba,
  // asignando el idAutorizacion a lo recibido por el mensaje
  // Para ello conecte a la BD, prepareStatement() y ejecute correctamente
  // la actualización
  public void onMessage(Message inMessage) {
      TextMessage msg = null;

      try {
          if (inMessage instanceof TextMessage) {
              msg = (TextMessage) inMessage;
              logger.info("MESSAGE BEAN: Message received: " + msg.getText());
              int idauth = Integer.parseInt(msg.getText());

              // Comprobamos que el pago no ha sido anulado previamente
              PreparedStatement pstmt = null;
              Connection con = null;
              ResultSet rs = null;
              int ret = 0;

              con = getConnection();

              String select = SELECT_CODRESPUESTA_QRY;
              logger.warning(select);
              pstmt = con.prepareStatement(select);
              pstmt.setInt(1, idauth);
              rs = pstmt.executeQuery();
              if(rs.next()){
                String cod = rs.getString("codrespuesta");
                if(!cod.equals("999")){
                  ejecutarConsultaActualizacion(UPDATE_CANCELA_QRY, idauth);
                  ejecutarConsultaActualizacion(RECTIFICAR_SALDO_QRY, idauth);
                }
                logger.warning("El pago ya ha sido anulado previamente.");
              }
              logger.warning("El pago no existe.");

              pstmt.close();

          } else {
              logger.warning(
                      "Message of wrong type: "
                      + inMessage.getClass().getName());
          }
      } catch (JMSException e) {
          e.printStackTrace();
          mdc.setRollbackOnly();
      } catch (Throwable te) {
          te.printStackTrace();
      }
  }


}
