package ij.text;

	/** Plugins that implement this interface are notified when
		a table is opened, closed or updated. The 
		Plugins/Utilities/Monitor Events command uses this interface.
	*/
	public interface TableListener {

	public void TableUpdated(TextPanel table, String event, int row);

}
