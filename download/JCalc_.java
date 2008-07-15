import ij.*;
import ij.gui.*;
import java.awt.*;
import java.awt.event.*;
import ij.plugin.frame.*;

/**
 * @version  1.2 Date 21 Aug 2006 - uses absolute positioning
 *           1.1 Date 18 Aug 2006 - added KeyListener support
 *           1.0.5 Date 17 Aug 2006 - code improved by Wayne Raspband
 *           1.0 Date 15 Aug 2006
 * 
 * @author Dimiter Prodanov
 * 
 *  Catholic University of Louvaion - Brussels
 *
 *  Based on Mikael Bonnier's  Pocket Calculator 
 *  
 * @contents       This plugin is a programing example for the PluginFrame class
 *
 *
 * @license      This library is free software; you can redistribute it and/or
 *      modify it under the terms of the GNU Lesser General Public
 *      License as published by the Free Software Foundation; either
 *      version 2.1 of the License, or (at your option) any later version.
 *
 *      This library is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *       Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public
 *      License along with this library; if not, write to the Free Software
 *      Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */


public class JCalc_ extends PlugInFrame implements ActionListener, KeyListener {

       public final int WINDOW_WIDTH = 300;
       public final int WINDOW_HEIGHT = 275;
       public final int OP_NONE = 0;
       public final int OP_ADD = 1;
       public final int OP_SUB = 2;
       public final int OP_MUL = 3;
       public final int OP_DIV = 4;
       public final int OP_NEG = 5;
       public final int OP_SQRT = 6;
       public final int OP_SQR = 7;
       public final int OP_LOG = 8;
       public final int OP_XDIV = 9;
       public final int OP_PI = 10;
       public final int OP_EQ = 70;
       public final int OP_C = 80;
       public final int OP_AC = 90;
       public final int OP_MC = 100;
       public final int OP_MR = 110;
       public final int OP_MM = 120;
       public final int OP_MP = 130;
       public final int OP_PCT = 14;
       public final int DECSEP = -1;

       TextField text;
       int buttonWidth, buttonHeight;
       String msDecimal;
       int mnOp = OP_NONE;
       boolean mbNewNumber = true;
       boolean mbDecimal = false;
       double mdReg = 0.0;
       double mdMemory = 0.0;
       boolean mbConstant = false;
       double mdConstant = 0.0;
       int mnConstantOp = OP_NONE;
       boolean mbPercent = false;
       double mdFirst = 0.0;
       static PlugInFrame instance;

    public JCalc_() {
        super("JCalc Plugin");
        if (instance!=null) {
            instance.toFront();
            return;
        }
        WindowManager.addWindow(this);
        instance = this;
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        GUI.center(this);
        setVisible(true);
    }

    /** Overrides Component.addNotify(). Init() must be called after 
        addNotify() or getInsets() will not return the title bar height. */
    public void addNotify() {
        super.addNotify();
        init();
    }

    public void init() {
          int  titleBarHeight = getInsets().top;
          int rows = 5;
          int columns = 5;
          buttonWidth = 50;
          buttonHeight = 34;
          int xm = 20; // x margin
          int ym =  titleBarHeight+38; // y margin
          int xinc = (WINDOW_WIDTH-xm*2)/columns+1;
          int yinc = (WINDOW_HEIGHT-ym)/rows-2;

          Button btn0 = makeButton("0", OP_NONE, xm, ym+yinc*3); 
          Button btn1 = makeButton("1", OP_NONE, xm, ym);
          Button btn2 = makeButton("2", OP_NONE, xm+xinc, ym);  
          Button btn3 = makeButton("3", OP_NONE, xm+xinc*2, ym); 
          Button btn4 = makeButton("4", OP_NONE, xm, ym+yinc); 
          Button btn5 = makeButton("5", OP_NONE, xm+xinc, ym+yinc);
          Button btn6 = makeButton("6", OP_NONE, xm+xinc*2, ym+yinc);  
          Button btn7 = makeButton("7", OP_NONE, xm, ym+yinc*2);   
          Button btn8 = makeButton("8", OP_NONE, xm+xinc, ym+yinc*2); 
          Button btn9 = makeButton("9", OP_NONE, xm+xinc*2, ym+yinc*2);
          //Button btnPI = makeButton("PI", OP_PI); 
          // modifiers
          Button btnDecSep = makeButton(".", OP_NONE, xm+xinc, ym+yinc*3);
          Button btnNeg = makeButton("\u00b1", OP_NEG, xm+xinc*2, ym+yinc*3); // plus-minus sign
          // operands
          Button btnSqrt = makeButton("Sqrt", OP_SQRT, xm+xinc*3, ym+yinc*4); 
          Button btnPlus = makeButton("+", OP_ADD, xm+xinc*3, ym);
          Button btnMinus = makeButton("-", OP_SUB, xm+xinc*3, ym+yinc);
          Button btnTimes = makeButton("x", OP_MUL, xm+xinc*3, ym+yinc*2); 
          Button btnDiv = makeButton("\u00f7", OP_DIV, xm+xinc*3, ym+yinc*3);
          Button btnEqual = makeButton("=", OP_EQ, xm+xinc*4, ym+yinc*4); 
          Button btnClear = makeButton("C", OP_C, xm+xinc*4, ym);
          //Button btnAllClear = makeButton("AC", OP_AC); 
          Button btnMemoryClear = makeButton("MC", OP_MC, xm+xinc*4, ym+yinc*2);
          Button btnMemoryRecall = makeButton("MR", OP_MR, xm+xinc*4, ym+yinc*3); 
          //Button btnMemoryMinus = makeButton("M-", OP_MM); 
          Button btnMemoryPlus = makeButton("M+", OP_MP, xm+xinc*4, ym+yinc);
          //Button btnPercent = makeButton("%", OP_PCT, xm, ym+yinc*4);
          Button btn1ovX = makeButton("1/x", OP_XDIV, xm+xinc, ym+yinc*4);
          Button btnLog = makeButton("Log", OP_LOG, xm, ym+yinc*4);; 
          Button btnSqr = makeButton("x\u00b2", OP_SQR, xm+xinc*2, ym+yinc*4); // "\u00b2" = superscript 2

          setLayout(null);
          setForeground(java.awt.Color.darkGray);
          setResizable(false);
          setFont(new Font("Helvetica", Font.PLAIN, 14));

          text = new TextField("0", 80);
          text.setEditable(false);
          text.addKeyListener(this);
             text.setBounds(xm, ym-30, WINDOW_WIDTH-xm*2, 25);
         add(text);

          String sOneTenth = (new Double(0.1)).toString();
          msDecimal = sOneTenth.substring(sOneTenth.length()-2).substring(0, 1);
             // Handles language dependent decimal separator.
       }


    Button makeButton(String label, int op, int x, int y) {
        Button button = new Button(label);
        button.addActionListener(this);
        button.addKeyListener(this);
        button.setName(""+op);
        if (label.length()>2)
                button.setFont(new Font("Helvetica", Font.PLAIN, 14));
        else
                button.setFont(new Font("Monospaced", Font.PLAIN, 16));
        button.setBounds(x, y, buttonWidth, buttonHeight);
        add(button);
        return button;
    }     

    public void actionPerformed(ActionEvent evt) {
        Button b = (Button)evt.getSource();
        int op = Integer.parseInt(b.getName());
        if (op==OP_NONE) {
            String cmd = evt.getActionCommand();
            doOp(OP_NONE);
            if (cmd.equals("."))
                append(DECSEP);
            else
                append(cmd);
        } else
            doOp(op);
      }

       public void append(String value) {
             append(Integer.parseInt(value));
          }

       public void append(int nValue) {
          String sDigit;

          if(nValue == DECSEP)
             if(!mbDecimal) {
                if(mbNewNumber) {
                   text.setText("0");
                   mbNewNumber = false;
                }
                mbDecimal = true;
                sDigit = msDecimal;
             }
             else
                return; 
          else
             sDigit = (new Integer(nValue)).toString();
          if(mbNewNumber) {
             text.setText(sDigit);
             if(nValue != 0)
                mbNewNumber = false;
          }
          else
             text.setText(text.getText() + sDigit);
          //repaint();
       }

       public void doOp(int nNewOp) {
          double dDisp;

          dDisp = (new Double(text.getText())).doubleValue();
          if(mbPercent)
             if(nNewOp != OP_ADD && nNewOp != OP_SUB)
                mbPercent = false;
          if(!mbPercent)
             switch(nNewOp) {
             case OP_ADD:
             case OP_SUB:
             case OP_MUL:
             case OP_DIV:
                if(mbNewNumber) {
                   if(nNewOp == mnOp && !mbConstant) {
                      mbConstant = true;
                      mdConstant = dDisp;
                      mnConstantOp = nNewOp;
                   }
                   else
                      mbConstant = false;
                }
                else
                   mbConstant = false;
             case OP_EQ:
             case OP_MM:
             case OP_MP:
             case OP_PCT:
                if(!mbNewNumber || isEqOp(nNewOp)) {
                   if(mbConstant) {
                      mdReg = mdConstant;
                      mnOp = mnConstantOp;
                   }
                   mbPercent = nNewOp == OP_PCT;
                   if(mbPercent)
                      mdFirst = mdReg;
                   switch(mnOp) {
                   case OP_ADD:
                      mdReg = mdReg + dDisp;
                      break;
                   case OP_SUB:
                      mdReg = mdReg - dDisp;
                      break;
                   case OP_MUL:
                      mdReg = mdReg * dDisp;
                      break;
                   case OP_DIV:
                      mdReg = mdReg / dDisp;
                      break;
                   case OP_EQ:
                   case OP_MM:
                   case OP_MP:
                   case OP_PCT:
                   case OP_NONE:
                      mdReg = dDisp;
                      break;
                   }
                   if(mbPercent)
                      mdReg /= 100;
                   text.setText((new Double(mdReg)).toString());
                }
                mnOp = nNewOp;
                mbNewNumber = true;
                mbDecimal = false;
                break;
             }
          switch(nNewOp) {
          case OP_ADD:
             if(mbPercent) {
                mdReg = mdFirst + mdReg;
                text.setText((new Double(mdReg)).toString());
                mbPercent = false;
             }
             break;
          case OP_SUB:
             if(mbPercent) {
                mdReg = mdFirst - mdReg;
                text.setText((new Double(mdReg)).toString());
                mbPercent = false;
             }
             break;
          case OP_NEG:
             dDisp = -dDisp;
             text.setText((new Double(dDisp)).toString());
             if(isEqOp(mnOp))
                mdReg = dDisp;
             break;
          case OP_SQRT:
             dDisp = Math.sqrt(dDisp);
             text.setText((new Double(dDisp)).toString());
             if(isEqOp(mnOp))
                mdReg = dDisp;
             mbNewNumber = true;
             mbDecimal = false;
             break;
          case OP_SQR:
                 dDisp = dDisp*dDisp;
                 text.setText((new Double(dDisp)).toString());
                 if(isEqOp(mnOp))
                    mdReg = dDisp;
                 mbNewNumber = true;
                 mbDecimal = false;
                 break;
          case OP_XDIV:
                 dDisp = 1/dDisp;
                 text.setText((new Double(dDisp)).toString());
                 if(isEqOp(mnOp))
                    mdReg = dDisp;
                 mbNewNumber = true;
                 mbDecimal = false;
                 break;
          case OP_LOG:
                 dDisp = Math.log(dDisp);
                 text.setText((new Double(dDisp)).toString());
                 if(isEqOp(mnOp))
                    mdReg = dDisp;
                 mbNewNumber = true;
                 mbDecimal = false;
                 break;
          case OP_PI:
                 dDisp = Math.PI;
                 text.setText((new Double(dDisp)).toString());
                 if(isEqOp(mnOp))
                    mdReg = dDisp;
                 mbNewNumber = true;
                 mbDecimal = false;
                 break;       
          case OP_C:
             dDisp = 0.0;
             text.setText("0");
             if(isEqOp(mnOp))
                mdReg = dDisp;
             mbNewNumber = true;
             mbDecimal = false;
             break;
          case OP_AC:
             text.setText("0");
             mnOp = OP_NONE;
             mbNewNumber = true;
             mbDecimal = false;
             mdReg = 0.0;
             mbConstant = false;
             break;
          case OP_MC:
             mdMemory = 0.0;
             break;
          case OP_MR:
             dDisp = mdMemory;
             text.setText((new Double(dDisp)).toString());
             if(isEqOp(mnOp))
                mdReg = dDisp;
             mbNewNumber = true;
             mbDecimal = false;
             break;
          case OP_MM:
             mdMemory -= mdReg;
             break;
          case OP_MP:
             mdMemory += mdReg;
             break;
          }
       }

       private  boolean isEqOp(int nOp) {
          return nOp == OP_EQ || nOp == OP_MM 
             || nOp == OP_MP || nOp == OP_PCT;
       }
    
    /** Overrides windowClosing in PluginFrame. */
    public void windowClosing(WindowEvent e) {
        super.windowClosing(e);
        instance = null;
    }

    public void keyTyped(KeyEvent ev) {
        char c=ev.getKeyChar();
        int code=ev.getKeyCode();
        String  s=new String(""); 
        if (c>='0' && c<='9') {
            doOp(OP_NONE); 
            s+=c;
            append(s);
        } else if (c=='.') {
            doOp(OP_NONE); 
            append(DECSEP);
        }
        else if (c=='+') doOp(OP_ADD);
        else if (c=='-') doOp(OP_SUB);
        else if (c=='*') doOp(OP_MUL);
        else if (c=='/') doOp(OP_DIV);
        else if (c=='=') doOp(OP_EQ);
        else if (c=='q') doOp(OP_SQRT);
        else if (c=='s') doOp(OP_SQR);
        else if (c=='l') doOp(OP_LOG);
        else if (c=='x') doOp(OP_XDIV);
        else if (c=='n') doOp(OP_NEG);
        else if (c=='p') doOp(OP_PI);
        else if (c==' ') doOp(OP_C);
        else if (c=='%') doOp(OP_PCT);
    }

    public void keyPressed(KeyEvent ev) {}
    public void keyReleased(KeyEvent ev) {} 
    
}
