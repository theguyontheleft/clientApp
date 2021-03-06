package client.orders;

import java.text.DecimalFormat;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.foodnow.R;

/**
 * 
 * @author Miguel Suarez
 * @author James Dagres
 * @author Carl Barbee
 * @author Matt Luckam
 * 
 *         Tab contains all the items the user has added to their order and a
 *         total of that order in US dollars. If an item in the order is clicked
 *         it allows the user to remove it from their order. If the confirm
 *         button is pushed it will send the user to paypal to pay for their
 *         order. A confirmation alertbox with order number is shown when a user
 *         has sucesfully completed their order.
 * 
 */
@SuppressWarnings( { "unused", "unchecked", "static-access" } )
public class OrderTab extends ListActivity
{

    /**
     * controls process flow 1. paypal 2. send to server 3. confirm order
     */
    public static int nextStep;

    /**
     * async task sends data to the server
     */
    private static OrderTabAsyncTask orderToServer;

    /**
     * instance of the settings task
     */
    private static SharedPreferences preference_;

    // ////////////////
    // layout items //
    // ///////////////
    /**
     * Instance of the list view
     */
    private static ListView listView;
    /**
     * Footer for the total
     */
    private static TextView footer;
    /**
     * Confirm button
     */
    private static Button button;

    // //////////////
    // containers //
    // /////////////
    /**
     * Items entered by the user is stored in this ArrayList variable
     */
    private static ArrayList<String> list;
    /**
     * Declaring an ArrayAdapter to set items to ListView
     */
    private static ArrayAdapter<String> adapter;

    /**
     * alter confirming an addition to your plate
     */
    private AlertDialog.Builder alertbox;

    // /////////////////
    // number values //
    // ////////////////
    /**
     * current item number selected
     */
    private int currentNumber;
    /**
     * number of items in the order
     */
    private static int numberOfItemsOnPlate;
    /**
     * total of the order in dollars
     */
    private static Double total;

    /**
     * Called when the activity is first created
     */
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        // sets up layout
        setContentView( R.layout.activity_order_tab );
        button = (Button) findViewById( R.id.Button );
        button.setEnabled( false );
        footer = (TextView) findViewById( R.id.Footer );
        listView = (ListView) findViewById( R.id.list );
        numberOfItemsOnPlate = 0;

        preference_ =
                getSharedPreferences( getString( R.string.pref_title_file ),
                        Context.MODE_PRIVATE );

        // sets up adapter
        list = new ArrayList<String>();
        adapter =
                new ArrayAdapter<String>( this, R.layout.list_view,
                        R.id.itemName, list );
        setListAdapter( adapter );

        onConfirmClick();
    }

    /**
     * Items added to the list - called from the menu
     * 
     * @param newItem
     *            String with new items name from the menu
     */
    public void addItems( String newItem )
    {
        // removes calorie and fat information
        newItem = newItem.substring( 0, newItem.indexOf( "$" ) + 5 );

        list.add( newItem );
        adapter.notifyDataSetChanged();
        button.setEnabled( true );
        numberOfItemsOnPlate++;

        calculateTotal();
    }

    /**
     * When confirm button is clicked
     */
    public void onConfirmClick()
    {
        button.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v )
            {
                // prepare the alert box
                AlertDialog.Builder alertbox =
                        new AlertDialog.Builder( OrderTab.this );
                // set the message to display
                alertbox.setMessage( "�Confirm your order?" );
                // set a positive/yes button and create a listener
                alertbox.setPositiveButton( "Yes",
                        new DialogInterface.OnClickListener()
                        {
                            // When order is confirmed
                            public void
                                    onClick( DialogInterface arg0, int arg1 )
                            {
                                String paymentMethod =
                                        OrderTab.this.preference_
                                                .getString(
                                                        getString( R.string.pref_title_payment ),
                                                        getString( R.string.pref_title_payment ) );

                                if ( paymentMethod.equals( "PayPal" ) )
                                {
                                    OrderTab.this.sendToPaypal();
                                }
                                else
                                {
                                    OrderTab.nextStep = 1;
                                }
                                OrderTab.this.sendToServer( paymentMethod );
                            }
                        } );

                // set a negative/no button and create a listener
                alertbox.setNegativeButton( "No", null );
                alertbox.show();
            }
        } );
    }

    /**
     * Settings for item that are removed from the list
     */
    private void removeItem()
    {
        // prepare the alert box
        alertbox = new AlertDialog.Builder( OrderTab.this );
        // set the message to display
        alertbox.setMessage( "�Remove from Plate?" );
        // set a positive/yes button and create a listener
        alertbox.setPositiveButton( "Yes",
                new DialogInterface.OnClickListener()
                {
                    public void onClick( DialogInterface arg0, int arg1 )
                    {
                        list.remove( currentNumber );
                        adapter.notifyDataSetChanged();

                        Toast.makeText( getApplicationContext(),
                                "The item was removed from your plate",
                                Toast.LENGTH_SHORT ).show();

                        numberOfItemsOnPlate--;
                        if ( numberOfItemsOnPlate <= 0 )
                        {
                            button.setEnabled( false );
                        }
                        calculateTotal();
                    }
                } );

        // set a negative/no button and create a listener
        alertbox.setNegativeButton( "No",
                new DialogInterface.OnClickListener()
                {
                    public void onClick( DialogInterface arg0, int arg1 )
                    {
                        Toast.makeText( getApplicationContext(),
                                "The item was NOT removed from your plate",
                                Toast.LENGTH_SHORT ).show();
                    }
                } );

        alertbox.show();
    }

    /**
     * sends order total to paypal for payment
     */
    private void sendToPaypal()
    {
        Intent in = new Intent( OrderTab.this, PaypalPaymentActivity.class );
        in.putExtra( "orderTotal", total );
        OrderTab.this.startActivity( in );
    }

    /**
     * sends order and username to server after payment information has been
     * sent to paypal
     */
    private void sendToServer( String paymentMethod )
    {
        String userName =
                preference_.getString( getString( R.string.pref_title_name ),
                        getString( R.string.pref_title_name ) );

        String phoneNumber =
                "1"
                        + preference_.getString(
                                getString( R.string.pref_title_phone_number ),
                                getString( R.string.pref_title_phone_number ) );

        orderToServer = new OrderTabAsyncTask( this );
        orderToServer.execute( list.toString(), userName, total.toString(),
                phoneNumber, paymentMethod );
    }

    /**
     * confirms the order after payment has been received and order has been
     * sent to the server
     */
    public void orderConfirmation()
    {
        // get prompts.xml view
        LayoutInflater li = LayoutInflater.from( getBaseContext() );
        View promptsView = li.inflate( R.layout.dialog_order_confirmed, null );

        // prepare the alert box
        AlertDialog.Builder alertbox = new AlertDialog.Builder( OrderTab.this );

        alertbox.setView( promptsView );
        alertbox.setTitle( "Order Confirmed" );

        // Display text prompting the user that the order was confirmed
        final TextView confirmationTextView;
        confirmationTextView =
                (TextView) promptsView
                        .findViewById( R.id.orderconfirmationTextView );
        confirmationTextView.setText( "Your order has been confirmed" );

        final TextView orderNumberTextView;
        orderNumberTextView =
                (TextView) promptsView
                        .findViewById( R.id.confirmationTextView );
        orderNumberTextView.setText( "Order ID: "
                + orderToServer.getOrderNumber() );

        alertbox.setPositiveButton( "Ok",
                new DialogInterface.OnClickListener()
                {
                    // after order completion resets the order
                    public void onClick( DialogInterface arg0, int arg1 )
                    {
                        list.clear();
                        total = 0.0;
                        adapter.notifyDataSetChanged();
                        footer.setText( "" );
                        button.setEnabled( false );
                        numberOfItemsOnPlate = 0;
                        nextStep = 0;
                    }
                } );
        alertbox.show();
    }

    /**
     * Lets the user know there order was not confirmed
     */
    public void orderFailed()
    {
        // get prompts.xml view
        LayoutInflater li = LayoutInflater.from( getBaseContext() );
        View promptsView = li.inflate( R.layout.dialog_order_failed, null );

        // prepare the alert box
        AlertDialog.Builder alertbox = new AlertDialog.Builder( OrderTab.this );

        alertbox.setView( promptsView );
        alertbox.setTitle( "Order Failed" );

        // Display text prompting the user that the order failed
        final TextView confirmationTextView;
        confirmationTextView =
                (TextView) promptsView
                        .findViewById( R.id.orderFailedTextView );
        confirmationTextView
                .setText( "Your order did not go through.\nSorry for the inconvenience." );

        // set a positive/yes button and create a listener
        alertbox.setPositiveButton( "Ok",
                new DialogInterface.OnClickListener()
                {
                    // after order completion resets the order
                    public void onClick( DialogInterface arg0, int arg1 )
                    {
                        nextStep = 0;
                    }
                } );
        alertbox.show();
    }

    /**
     * recalculates total and displays it
     */
    private void calculateTotal()
    {
        total = 0.0;
        for ( int i = 0; i < list.size(); i++ )
        {
            total +=
                    Double.parseDouble( list.get( i ).substring(
                            list.get( i ).indexOf( "$" ) + 1 ) );

        }
        footer.setTextSize( 25 );
        DecimalFormat twoDForm = new DecimalFormat( "#.##" );
        total = Double.valueOf( twoDForm.format( total ) );

        footer.setText( "Total: $" + total );

    }

    /**
     * When list view item is clicked
     */
    @Override
    protected void onListItemClick( ListView l, View v, int position, long id )
    {
        currentNumber = position;

        removeItem();
    }
}