/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package ij.plugin;

import ij.*;

import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Set;

/** This is a plugin that provides an easy user interface to finding
    commands you might know the name of without having to go through
    all the menus.  If you type a part of a command name, the box
    below will only show commands that match that substring (case
    insensitively). */

public class Command_Finder implements PlugIn, TextListener, ActionListener, WindowListener, KeyListener {

	Dialog d;

	TextField prompt;

	List completions;

	Button runButton;
	Button cancelButton;

	Hashtable commandsHash;
	String [] commands;
	Hashtable listLabelToCommand;

	public String makeListLabel( String command, String className ) {
		return command + "  [" + className + "]";
	}

	public void populateList( String matchingSubstring ) {

		String substring = matchingSubstring.toLowerCase();

		completions.removeAll();

		for( int i = 0; i < commands.length; ++i ) {
			String commandName = commands[i];
			if( commandName.length() == 0 )
				continue;
			String lowerCommandName = commandName.toLowerCase();
			String className = (String) commandsHash.get(commandName);
			if( lowerCommandName.indexOf(substring) >= 0 ) {
				String listLabel = makeListLabel(commandName,className);
				completions.add(listLabel);
			}
		}
	}

	public void actionPerformed( ActionEvent ae ) {
		Object source = ae.getSource();
		if( source == runButton ) {
			String selected = completions.getSelectedItem();
			if( selected == null ) {
				IJ.error("You must select a plugin to run");
				return;
			}
			runFromLabel( selected );
		} else if( source == cancelButton ) {
			d.dispose();
		}
	}

	public void runFromLabel( String listLabel ) {
		String commandName = (String) listLabelToCommand.get( listLabel );
		IJ.showStatus( "Running "+commandName );
		IJ.doCommand( commandName );
		d.dispose();
	}

	public void keyPressed( KeyEvent ke ) {
		int key = ke.getKeyCode();
		int items = completions.getItemCount();
		Object source = ke.getSource();
		if( source == prompt ) {
			if ( key == KeyEvent.VK_ENTER ) {
				if( 1 == items ) {
					String selected = completions.getItem( 0 );
					runFromLabel( selected );
				}
			} else if( key == KeyEvent.VK_UP ) {
				completions.requestFocus();
				if( items > 0 )
					completions.select( completions.getItemCount() - 1 );
			} else if( key == KeyEvent.VK_DOWN )  {
				completions.requestFocus();
				if( items > 0 )
					completions.select( 0 );
			}
		} else if( source == completions ) {
			if ( key == KeyEvent.VK_ENTER ) {
				String selected = completions.getSelectedItem();
				if( selected != null )
					runFromLabel( selected );
			}
		}
	}

	public void keyReleased( KeyEvent ke ) { }

	public void keyTyped( KeyEvent ke ) { }

	public void textValueChanged( TextEvent te ) {
		populateList( prompt.getText() );
	}

	public void run( String ignored ) {

		commandsHash = ij.Menus.getCommands();

		Set commandSet = commandsHash.keySet();

		ArrayList nonEmptyCommands = new ArrayList();
		for( Iterator i = commandSet.iterator();
		     i.hasNext(); ) {
			String command = (String) i.next();
			String trimmedCommand = command.trim();
			if( trimmedCommand.length() > 0 )
				nonEmptyCommands.add( command );
		}

		commands = (String[])nonEmptyCommands.toArray( new String[0] );
		Arrays.sort( commands );

		listLabelToCommand = new Hashtable();

		for( int i = 0; i < commands.length; ++i ) {
			commands[i] = commands[i].trim();
			String command = commands[i];
			String className = (String) commandsHash.get( command );
			String listLabel = makeListLabel( command, className );
			if( commands.length > 0 )
				listLabelToCommand.put( listLabel, command );
		}

		ImageJ imageJ = IJ.getInstance();

		d = new Dialog( imageJ, "Command Finder" );

		d.setLayout( new BorderLayout() );

		d.addWindowListener( this );

		Panel northPanel = new Panel();

		northPanel.add( new Label("Type part of a command:") );

		prompt = new TextField( "", 40 );
		prompt.addTextListener( this );
		prompt.addKeyListener( this );

		northPanel.add( prompt );

		d.add( northPanel, BorderLayout.NORTH );

		completions = new List( 20 );
		// completions.addItemListener( this );
		completions.addKeyListener( this );
		populateList( "" );

		d.add( completions, BorderLayout.CENTER );

		runButton = new Button( "Run" );
		cancelButton = new Button( "Cancel" );

		runButton.addActionListener( this );
		cancelButton.addActionListener( this );

		Panel p = new Panel();
		p.add( runButton );
		p.add( cancelButton );

		d.add( p, BorderLayout.SOUTH );

		int offsetX = 38;
		int offsetY = 84;

		Point imageJPosition=imageJ.getLocationOnScreen();

		d.setLocation( (int) imageJPosition.getX() + 38,
			       (int) imageJPosition.getY() + 84);

		d.pack();
		d.setVisible( true );

	}

	public void windowClosing( WindowEvent e ) {
		d.dispose();
	}

	public void windowActivated( WindowEvent e ) { }
	public void windowDeactivated( WindowEvent e ) { }
	public void windowClosed( WindowEvent e ) { }
	public void windowOpened( WindowEvent e ) { }
	public void windowIconified( WindowEvent e ) { }
	public void windowDeiconified( WindowEvent e ) { }
}
