/*
 *  soapUI Pro, copyright (C) 2007-2009 eviware software ab 
 */

package com.eviware.soapui.impl.wsdl.panels.teststeps.amf;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.Document;

import org.apache.log4j.Logger;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.AMFRequestTestStepConfig;
import com.eviware.soapui.impl.rest.RestRequestInterface;
import com.eviware.soapui.impl.support.actions.ShowOnlineHelpAction;
import com.eviware.soapui.impl.support.components.ModelItemXmlEditor;
import com.eviware.soapui.impl.support.components.ResponseMessageXmlEditor;
import com.eviware.soapui.impl.support.panels.AbstractHttpRequestDesktopPanel;
import com.eviware.soapui.impl.wsdl.panels.support.MockTestRunContext;
import com.eviware.soapui.impl.wsdl.panels.support.MockTestRunner;
import com.eviware.soapui.impl.wsdl.panels.teststeps.AssertionsPanel;
import com.eviware.soapui.impl.wsdl.panels.teststeps.support.GroovyEditor;
import com.eviware.soapui.impl.wsdl.panels.teststeps.support.GroovyEditorModel;
import com.eviware.soapui.impl.wsdl.panels.teststeps.support.PropertyHolderTable;
import com.eviware.soapui.impl.wsdl.support.HelpUrls;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestRunContext;
import com.eviware.soapui.impl.wsdl.teststeps.AMFRequestTestStep;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;
import com.eviware.soapui.impl.wsdl.teststeps.actions.AddAssertionAction;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.iface.Submit;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.iface.SubmitListener;
import com.eviware.soapui.model.iface.Request.SubmitException;
import com.eviware.soapui.model.iface.Submit.Status;
import com.eviware.soapui.model.settings.Settings;
import com.eviware.soapui.model.testsuite.Assertable;
import com.eviware.soapui.model.testsuite.AssertionsListener;
import com.eviware.soapui.model.testsuite.LoadTestRunner;
import com.eviware.soapui.model.testsuite.TestAssertion;
import com.eviware.soapui.model.testsuite.TestCaseRunner;
import com.eviware.soapui.model.testsuite.Assertable.AssertionStatus;
import com.eviware.soapui.monitor.support.TestMonitorListenerAdapter;
import com.eviware.soapui.settings.UISettings;
import com.eviware.soapui.support.DocumentListenerAdapter;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.actions.ChangeSplitPaneOrientationAction;
import com.eviware.soapui.support.components.JComponentInspector;
import com.eviware.soapui.support.components.JEditorStatusBarWithProgress;
import com.eviware.soapui.support.components.JInspectorPanel;
import com.eviware.soapui.support.components.JInspectorPanelFactory;
import com.eviware.soapui.support.components.JUndoableTextField;
import com.eviware.soapui.support.components.JXToolBar;
import com.eviware.soapui.support.components.SimpleForm;
import com.eviware.soapui.support.editor.xml.support.AbstractXmlDocument;
import com.eviware.soapui.support.propertyexpansion.PropertyExpansionPopupListener;
import com.eviware.soapui.support.scripting.SoapUIScriptEngine;
import com.eviware.soapui.support.scripting.SoapUIScriptEngineRegistry;
import com.eviware.soapui.ui.support.ModelItemDesktopPanel;

public class AMFRequestTestStepDesktopPanel extends ModelItemDesktopPanel<AMFRequestTestStep> implements SubmitListener
{
	private static final String AMF_CALL = "AMF Call";
	private final static Logger log = Logger.getLogger( AbstractHttpRequestDesktopPanel.class );
	private JPanel configPanel;
	private JButton addAssertionButton;
	private JInspectorPanel inspectorPanel;
	private AMFRequestTestStep amfRequestTestStep;
	protected AMFRequestTestStepConfig amfRequestTestStepConfig;
	private JComponentInspector<?> assertionInspector;
	private AssertionsPanel assertionsPanel;
	private InternalAssertionsListener assertionsListener = new InternalAssertionsListener();
	private InternalTestMonitorListener testMonitorListener = new InternalTestMonitorListener();
	private JComponent requestEditor;
	private ModelItemXmlEditor<?, ?> responseEditor;
	private JPanel panel;
	private Submit submit;
	private JButton submitButton;
	private JToggleButton tabsButton;
	private JTabbedPane requestTabs;
	private JPanel requestTabPanel;
	private boolean responseHasFocus;
	private JSplitPane requestSplitPane;
	private JEditorStatusBarWithProgress statusBar;
	private JButton cancelButton;
	private JButton splitButton;
	private JComponent propertiesTableComponent;
	private SoapUIScriptEngine scriptEngine;
	private RunAction runAction = new RunAction();
	private GroovyEditor groovyEditor;
	private JTextField amfCall;
	public boolean updating;
	SimpleForm configForm;

	public AMFRequestTestStepDesktopPanel( AMFRequestTestStep modelItem )
	{
		super( modelItem );
		amfRequestTestStep = modelItem;
		initConfig();
		initContent();

		SoapUI.getTestMonitor().addTestMonitorListener( testMonitorListener );
		setEnabled( !SoapUI.getTestMonitor().hasRunningTest( amfRequestTestStep.getTestCase() ) );

		amfRequestTestStep.addAssertionsListener( assertionsListener );

		scriptEngine = SoapUIScriptEngineRegistry.create( modelItem );
		scriptEngine.setScript( amfRequestTestStep.getScript() );
	}

	protected void initConfig()
	{
		amfRequestTestStepConfig = amfRequestTestStep.getAMFRequestTestStepConfig();
	}

	private JComponent buildContent()
	{
		requestSplitPane = UISupport.createHorizontalSplit();
		requestSplitPane.setResizeWeight( 0.5 );
		requestSplitPane.setBorder( null );

		JComponent content;
		submitButton = createActionButton( new SubmitAction(), true );
		cancelButton = createActionButton( new CancelAction(), false );
		tabsButton = new JToggleButton( new ChangeToTabsAction() );
		tabsButton.setPreferredSize( UISupport.TOOLBAR_BUTTON_DIMENSION );
		splitButton = createActionButton( new ChangeSplitPaneOrientationAction( requestSplitPane ), true );

		addAssertionButton = UISupport.createToolbarButton( new AddAssertionAction( amfRequestTestStep ) );
		addAssertionButton.setEnabled( true );

		requestTabs = new JTabbedPane();
		requestTabs.addChangeListener( new ChangeListener()
		{

			public void stateChanged( ChangeEvent e )
			{
				SwingUtilities.invokeLater( new Runnable()
				{

					public void run()
					{
						int ix = requestTabs.getSelectedIndex();
						if( ix == 0 )
							requestEditor.requestFocus();
						else if( ix == 1 && responseEditor != null )
							responseEditor.requestFocus();
					}
				} );
			}
		} );

		addFocusListener( new FocusAdapter()
		{

			@Override
			public void focusGained( FocusEvent e )
			{
				if( requestTabs.getSelectedIndex() == 1 || responseHasFocus )
					responseEditor.requestFocusInWindow();
				else
					requestEditor.requestFocusInWindow();
			}
		} );

		requestTabPanel = UISupport.createTabPanel( requestTabs, true );

		requestEditor = buildRequestConfigPanel();
		responseEditor = buildResponseEditor();
		if( amfRequestTestStep.getSettings().getBoolean( UISettings.START_WITH_REQUEST_TABS ) )
		{
			requestTabs.addTab( "Request", requestEditor );
			if( responseEditor != null )
				requestTabs.addTab( "Response", responseEditor );
			tabsButton.setSelected( true );
			splitButton.setEnabled( false );

			content = requestTabPanel;
		}
		else
		{
			requestSplitPane.setTopComponent( requestEditor );
			requestSplitPane.setBottomComponent( responseEditor );
			requestSplitPane.setDividerLocation( 0.5 );
			content = requestSplitPane;
		}

		inspectorPanel = JInspectorPanelFactory.build( content );
		inspectorPanel.setDefaultDividerLocation( 0.7F );
		add( buildToolbar(), BorderLayout.NORTH );
		add( inspectorPanel.getComponent(), BorderLayout.CENTER );
		assertionsPanel = buildAssertionsPanel();

		assertionInspector = new JComponentInspector<JComponent>( assertionsPanel, "Assertions ("
				+ getModelItem().getAssertionCount() + ")", "Assertions for this Test Request", true );

		inspectorPanel.addInspector( assertionInspector );
		// setPreferredSize(new Dimension(600, 450));

		updateStatusIcon();

		return inspectorPanel.getComponent();
	}

	protected JComponent buildRequestConfigPanel()
	{
		configPanel = UISupport.addTitledBorder( new JPanel( new BorderLayout() ), "AMF call and intialisation groovy script" );
		if( panel == null )
		{
			panel = new JPanel( new BorderLayout() );
			configForm = new SimpleForm();
			
			addAmfCAlltoSimpleForm();
			addGroovyEditorToSimpleForm();
			
			panel.add( configForm.getPanel() );
		}
		configPanel.add( panel, BorderLayout.CENTER );
		propertiesTableComponent = buildProperties();
		JSplitPane split = UISupport.createVerticalSplit( propertiesTableComponent, configPanel );
		split.setDividerLocation( 120 );

		// TODO add scrolling but without messing with the dimension - ask Ole
		return split;

	}

	protected void initContent()
	{
		amfRequestTestStep.getAMFRequest().addSubmitListener( this );

		add( buildContent(), BorderLayout.CENTER );
		add( buildToolbar(), BorderLayout.NORTH );
		add( buildStatusLabel(), BorderLayout.SOUTH );

		setPreferredSize( new Dimension( 600, 500 ) );

		addFocusListener( new FocusAdapter()
		{

			@Override
			public void focusGained( FocusEvent e )
			{
				if( requestTabs.getSelectedIndex() == 1 || responseHasFocus )
					responseEditor.requestFocusInWindow();
				else
					requestEditor.requestFocusInWindow();
			}
		} );
	}

	protected JComponent buildStatusLabel()
	{
		statusBar = new JEditorStatusBarWithProgress();
		statusBar.setBorder( BorderFactory.createEmptyBorder( 1, 0, 0, 0 ) );

		return statusBar;
	}

	protected JComponent buildProperties()
	{
		PropertyHolderTable holderTable = new PropertyHolderTable( getModelItem() );

		JUndoableTextField textField = new JUndoableTextField( true );

		PropertyExpansionPopupListener.enable( textField, getModelItem() );
		holderTable.getPropertiesTable().setDefaultEditor( String.class, new DefaultCellEditor( textField ) );

		return holderTable;
	}

	protected JComponent buildToolbar()
	{
		JXToolBar toolbar = UISupport.createToolbar();

		toolbar.setBorder( BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) );

		toolbar.addFixed( submitButton );
		toolbar.add( cancelButton );
		toolbar.addFixed( addAssertionButton );

		toolbar.add( Box.createHorizontalGlue() );
		toolbar.add( tabsButton );
		toolbar.add( splitButton );
		toolbar.addFixed( UISupport
				.createToolbarButton( new ShowOnlineHelpAction( HelpUrls.TRANSFERSTEPEDITOR_HELP_URL ) ) );
		return toolbar;

	}

	public AMFRequestTestStep getAMFRequestTestStep()
	{
		return amfRequestTestStep;
	}

	protected AssertionsPanel buildAssertionsPanel()
	{
		return new AMFAssertionsPanel( amfRequestTestStep )
		{
			// protected void selectError( AssertionError error )
			// {
			// ModelItemXmlEditor<?, ?> editor = ( ModelItemXmlEditor<?, ?>
			// ).getResultEditorModel();
			// editor.requestFocus();
			// }
		};
	}

	protected class AMFAssertionsPanel extends AssertionsPanel
	{
		public AMFAssertionsPanel( Assertable assertable )
		{
			super( assertable );
//			addAssertionAction = new AddAssertionAction( assertable );
//			assertionListPopup.add( addAssertionAction );
		}
	}

	protected SimpleForm addGroovyEditorToSimpleForm()
	{
		configForm.addSpace( 5 );
		configForm.append( "Groovy Script", groovyEditor = new GroovyEditor( new ScriptStepGroovyEditorModel() ) );
		return configForm;
	}

	private void addAmfCAlltoSimpleForm()
	{
		configForm.addSpace( 5 );
		amfCall = configForm.appendTextField( AMF_CALL, "object.methodName for amf method call" );
		amfCall.setText( amfRequestTestStep.getAmfCall() );
		PropertyExpansionPopupListener.enable( amfCall, amfRequestTestStep );
		addAmfCallDocumentListener();
	}

	protected void addAmfCallDocumentListener()
	{
		amfCall.getDocument().addDocumentListener( new DocumentListenerAdapter()
		{
			@Override
			public void update( Document document )
			{
				if( !updating )
				{
					amfRequestTestStep.setAmfCall( configForm.getComponentValue( AMF_CALL ) );
				}
			}
		} );
	}

	private class ScriptStepGroovyEditorModel implements GroovyEditorModel
	{
		public String[] getKeywords()
		{
			return new String[] { "log", "context", "testRunner" };
		}

		public Action getRunAction()
		{
			return runAction;
		}

		public String getScript()
		{
			return amfRequestTestStep.getScript();
		}

		public void setScript( String text )
		{
			if( updating )
				return;

			updating = true;
			amfRequestTestStep.setScript( text );
			updating = false;
		}

		public Settings getSettings()
		{
			return SoapUI.getSettings();
		}

		public String getScriptName()
		{
			return null;
		}

		public void addPropertyChangeListener( PropertyChangeListener listener )
		{
		}

		public void removePropertyChangeListener( PropertyChangeListener listener )
		{
		}

		public ModelItem getModelItem()
		{
			return amfRequestTestStep;
		}
	}

	private class RunAction extends AbstractAction
	{
		public RunAction()
		{
			putValue( Action.SMALL_ICON, UISupport.createImageIcon( "/run_groovy_script.gif" ) );
			putValue( Action.SHORT_DESCRIPTION,
					"Runs this script in a seperate thread using a mock testRunner and testContext" );
		}

		public void actionPerformed( ActionEvent e )
		{
			SoapUI.getThreadPool().execute( new Runnable()
			{
				public void run()
				{
					MockTestRunner mockTestRunner = new MockTestRunner( amfRequestTestStep.getTestCase(), log );
					statusBar.setIndeterminate( true );
					WsdlTestStepResult result = ( WsdlTestStepResult )amfRequestTestStep.run( mockTestRunner,
							new MockTestRunContext( mockTestRunner, amfRequestTestStep ) );
					statusBar.setIndeterminate( false );

					Throwable er = result.getError();
					if( er != null )
					{
						String message = er.getMessage();

						// ugly...
						groovyEditor.selectError( message );

						UISupport.showErrorMessage( er.toString() );
						groovyEditor.requestFocus();
					}
					else if( result.getMessages().length > 0 )
					{
						UISupport.showInfoMessage( StringUtils.join( result.getMessages(), "\n" ) );
					}
				}
			} );
		}
	}

	protected ModelItemXmlEditor<?, ?> buildResponseEditor()
	{
		return new AMFResponseMessageEditor();
	}

	public class AMFResponseMessageEditor extends ResponseMessageXmlEditor<AMFRequestTestStep, AMFResponseDocument>
	{
		public AMFResponseMessageEditor()
		{
			super( new AMFResponseDocument(), amfRequestTestStep );
		}
	}

	public boolean dependsOn( ModelItem modelItem )
	{
		return modelItem == getModelItem() || modelItem == getModelItem().getTestCase()
				|| modelItem == getModelItem().getTestCase().getTestSuite()
				|| modelItem == getModelItem().getTestCase().getTestSuite().getProject();
	}

	public boolean onClose( boolean canCancel )
	{
		configPanel.removeAll();
		inspectorPanel.release();

		SoapUI.getTestMonitor().removeTestMonitorListener( testMonitorListener );
		amfRequestTestStep.removeAssertionsListener( assertionsListener );

		return release();
	}

	public class AMFResponseDocument extends AbstractXmlDocument implements PropertyChangeListener
	{
		public AMFResponseDocument()
		{
			amfRequestTestStep.addPropertyChangeListener( AMFRequestTestStep.RESPONSE_PROPERTY, this );
		}

		public void propertyChange( PropertyChangeEvent evt )
		{
			fireXmlChanged( evt.getOldValue() == null ? null : ( ( AMFResponse )evt.getOldValue() ).getContentAsString(),
					getXml() );
		}

		public String getXml()
		{
			AMFResponse response = amfRequestTestStep.getAMFRequest().getResponse();
			return response == null ? null : response.getContentAsString();
		}

		public void setXml( String xml )
		{
			if( amfRequestTestStep.getAMFRequest().getResponse() != null )
				amfRequestTestStep.getAMFRequest().getResponse().setContentAsString( xml );
		}

		public void release()
		{
			super.release();
			amfRequestTestStep.removePropertyChangeListener( RestRequestInterface.RESPONSE_PROPERTY, this );
		}
	}

	private String executeScript()
	{
		try
		{
			scriptEngine.setScript( groovyEditor.getEditArea().getText() );
			// scriptEngine.setVariable("context", context);
			// scriptEngine.setVariable("messageExchange", messageExchange);
			scriptEngine.setVariable( "log", log );

			Object result = scriptEngine.run();
			return result == null ? null : result.toString();
		}
		catch( Throwable e )
		{
			SoapUI.logError( e );
		}
		finally
		{
			scriptEngine.clearVariables();
		}
		return null;
	}

	public class TestConnectionAction extends AbstractAction
	{
		public TestConnectionAction()
		{
			putValue( Action.SMALL_ICON, UISupport.createImageIcon( "/run_testcase.gif" ) );
			putValue( Action.SHORT_DESCRIPTION, "Test the current Connection" );

			setEnabled( false );
		}

		public void actionPerformed( ActionEvent arg0 )
		{
			try
			{
				// AMFUtils.testConnection(getModelItem(),
				// amfRequestTestStep.getDriver(), amfRequestTestStep
				// .getConnectionString(), amfRequestTestStep.getPassword());
				UISupport.showInfoMessage( "The Connection Successfully Tested" );
			}
			catch( Exception e )
			{
				UISupport.showErrorMessage( "Can't get the Connection for specified properties; " + e.toString() );
			}
		}
	}

	private class InternalTestMonitorListener extends TestMonitorListenerAdapter
	{
		public void loadTestFinished( LoadTestRunner runner )
		{
			setEnabled( !SoapUI.getTestMonitor().hasRunningTest( getModelItem().getTestCase() ) );
		}

		public void loadTestStarted( LoadTestRunner runner )
		{
			if( runner.getLoadTest().getTestCase() == getModelItem().getTestCase() )
				setEnabled( false );
		}

		public void testCaseFinished( TestCaseRunner runner )
		{
			setEnabled( !SoapUI.getTestMonitor().hasRunningTest( getModelItem().getTestCase() ) );
		}

		public void testCaseStarted( TestCaseRunner runner )
		{
			if( runner.getTestCase() == getModelItem().getTestCase() )
				setEnabled( false );
		}
	}

	public class SubmitAction extends AbstractAction
	{
		public SubmitAction()
		{
			putValue( Action.SMALL_ICON, UISupport.createImageIcon( "/submit_request.gif" ) );
			putValue( Action.SHORT_DESCRIPTION, "Submit request to specified endpoint URL" );
			putValue( Action.ACCELERATOR_KEY, UISupport.getKeyStroke( "alt ENTER" ) );
		}

		public void actionPerformed( ActionEvent e )
		{
			onSubmit();
		}
	}

	protected void onSubmit()
	{
		if( submit != null && submit.getStatus() == Submit.Status.RUNNING )
		{
			if( UISupport.confirm( "Cancel current request?", "Submit Request" ) )
			{
				submit.cancel();
			}
			else
				return;
		}

		try
		{
			submit = doSubmit();
		}
		catch( SubmitException e1 )
		{
			SoapUI.logError( e1 );
		}
	}

	protected Submit doSubmit() throws SubmitException
	{
		String temp = executeScript();
		return amfRequestTestStep.getAMFRequest().submit( new WsdlTestRunContext( getModelItem() ), true );
	}

	protected final class InputAreaFocusListener implements FocusListener
	{
		public InputAreaFocusListener( JComponent editor )
		{
		}

		public void focusGained( FocusEvent e )
		{
			responseHasFocus = false;

			// statusBar.setTarget(sourceEditor.getInputArea());
			if( !splitButton.isEnabled() )
			{
				requestTabs.setSelectedIndex( 0 );
				return;
			}

			// if
			// (getModelItem().getSettings().getBoolean(UISettings.NO_RESIZE_REQUEST_EDITOR))
			// return;

			// // dont resize if split has been dragged
			// if (requestSplitPane.getUI() instanceof SoapUISplitPaneUI
			// && ((SoapUISplitPaneUI) requestSplitPane.getUI()).hasBeenDragged())
			// return;
			//
			int pos = requestSplitPane.getDividerLocation();
			if( pos >= 600 )
				return;
			if( requestSplitPane.getMaximumDividerLocation() > 700 )
				requestSplitPane.setDividerLocation( 600 );
			else
				requestSplitPane.setDividerLocation( 0.8 );
		}

		public void focusLost( FocusEvent e )
		{
		}
	}

	protected final class ResultAreaFocusListener implements FocusListener
	{
		private final ModelItemXmlEditor<?, ?> responseEditor;

		public ResultAreaFocusListener( ModelItemXmlEditor<?, ?> editor )
		{
			this.responseEditor = editor;
		}

		public void focusGained( FocusEvent e )
		{
			responseHasFocus = true;

			// statusBar.setTarget(sourceEditor.getInputArea());
			if( !splitButton.isEnabled() )
			{
				requestTabs.setSelectedIndex( 1 );
				return;
			}
			//
			// if
			// (getModelItem().getSettings().getBoolean(UISettings.NO_RESIZE_REQUEST_EDITOR))
			// return;
			//
			// // dont resize if split has been dragged or result is empty
			// if (requestSplitPane.getUI() instanceof SoapUISplitPaneUI
			// && ((SoapUISplitPaneUI) requestSplitPane.getUI()).hasBeenDragged()
			// || request.getResponse() == null)
			// return;
			//
			int pos = requestSplitPane.getDividerLocation();
			int maximumDividerLocation = requestSplitPane.getMaximumDividerLocation();
			if( pos + 600 < maximumDividerLocation )
				return;

			if( maximumDividerLocation > 700 )
				requestSplitPane.setDividerLocation( maximumDividerLocation - 600 );
			else
				requestSplitPane.setDividerLocation( 0.2 );
		}

		public void focusLost( FocusEvent e )
		{
		}
	}

	private final class ChangeToTabsAction extends AbstractAction
	{
		public ChangeToTabsAction()
		{
			putValue( Action.SMALL_ICON, UISupport.createImageIcon( "/toggle_tabs.gif" ) );
			putValue( Action.SHORT_DESCRIPTION, "Toggles to tab-based layout" );
		}

		public void actionPerformed( ActionEvent e )
		{
			if( splitButton.isEnabled() )
			{
				splitButton.setEnabled( false );
				removeContent( requestSplitPane );
				setContent( requestTabPanel );
				requestTabs.addTab( "Request", requestEditor );

				if( responseEditor != null )
					requestTabs.addTab( "Response", responseEditor );

				if( responseHasFocus )
				{
					requestTabs.setSelectedIndex( 1 );
					requestEditor.requestFocus();
				}
				requestTabs.repaint();
			}
			else
			{
				int selectedIndex = requestTabs.getSelectedIndex();

				splitButton.setEnabled( true );
				removeContent( requestTabPanel );
				setContent( requestSplitPane );
				requestSplitPane.setTopComponent( requestEditor );
				if( responseEditor != null )
					requestSplitPane.setBottomComponent( responseEditor );
				requestSplitPane.setDividerLocation( 0.5 );

				if( selectedIndex == 0 || responseEditor == null )
					requestEditor.requestFocus();
				else
					responseEditor.requestFocus();
				requestSplitPane.repaint();
			}

			revalidate();
		}
	}

	public void setContent( JComponent content )
	{
		inspectorPanel.setContentComponent( content );
	}

	public void removeContent( JComponent content )
	{
		inspectorPanel.setContentComponent( null );
	}

	private class CancelAction extends AbstractAction
	{
		public CancelAction()
		{
			super();
			putValue( Action.SMALL_ICON, UISupport.createImageIcon( "/cancel_request.gif" ) );
			putValue( Action.SHORT_DESCRIPTION, "Aborts ongoing request" );
			putValue( Action.ACCELERATOR_KEY, UISupport.getKeyStroke( "alt X" ) );
		}

		public void actionPerformed( ActionEvent e )
		{
			onCancel();
		}
	}

	protected void onCancel()
	{
		if( submit == null )
			return;

		cancelButton.setEnabled( false );
		submit.cancel();
		setEnabled( true );
		submit = null;
	}

	public void setEnabled( boolean enabled )
	{
		if( responseEditor != null )
			responseEditor.setEditable( enabled );

		submitButton.setEnabled( enabled );
		addAssertionButton.setEnabled( enabled );
		propertiesTableComponent.setEnabled( enabled );

		statusBar.setIndeterminate( !enabled );
	}

	public void afterSubmit( Submit submit, SubmitContext context )
	{
		if( submit.getRequest() != amfRequestTestStep.getAMFRequest() )
			return;

		Status status = submit.getStatus();
		AMFResponse response = ( AMFResponse )submit.getResponse();
		if( status == Status.FINISHED )
		{
			amfRequestTestStep.setResponse( response, context );
		}

		cancelButton.setEnabled( false );
		setEnabled( true );

		String message = null;
		String infoMessage = null;
		String requestName = amfRequestTestStep.getName();

		if( status == Status.CANCELED )
		{
			message = "CANCELED";
			infoMessage = "[" + requestName + "] - CANCELED";
		}
		else
		{
			if( status == Status.ERROR || response == null )
			{
				message = "Error getting response; " + submit.getError();
				infoMessage = "Error getting response for [" + requestName + "]; " + submit.getError();
			}
			else
			{
				message = "response time: " + response.getTimeTaken() + "ms (" + response.getContentLength() + " bytes)";
				infoMessage = "Got response for [" + requestName + "] in " + response.getTimeTaken() + "ms ("
						+ response.getContentLength() + " bytes)";

				if( !splitButton.isEnabled() )
					requestTabs.setSelectedIndex( 1 );

				responseEditor.requestFocus();
			}
		}

		logMessages( message, infoMessage );

		if( getModelItem().getSettings().getBoolean( UISettings.AUTO_VALIDATE_RESPONSE ) )
			responseEditor.getSourceEditor().validate();

		AMFRequestTestStepDesktopPanel.this.submit = null;

		updateStatusIcon();
	}

	protected void logMessages( String message, String infoMessage )
	{
		log.info( infoMessage );
		statusBar.setInfo( message );
	}

	public boolean beforeSubmit( Submit submit, SubmitContext context )
	{
		if( submit.getRequest() != amfRequestTestStep.getAMFRequest() )
			return true;

		setEnabled( false );
		cancelButton.setEnabled( AMFRequestTestStepDesktopPanel.this.submit != null );
		return true;
	}

	public void propertyChange( PropertyChangeEvent evt )
	{
		super.propertyChange( evt );
		if( evt.getPropertyName().equals( "script" ) && !updating )
		{
			updating = true;
			groovyEditor.getEditArea().setText( ( String )evt.getNewValue() );
			updating = false;
		}
		if( evt.getPropertyName().equals( AMFRequestTestStep.STATUS_PROPERTY ) )
			updateStatusIcon();
	}

	private final class InternalAssertionsListener implements AssertionsListener
	{
		public void assertionAdded( TestAssertion assertion )
		{
			assertionInspector.setTitle( "Assertions (" + getModelItem().getAssertionCount() + ")" );
		}

		public void assertionRemoved( TestAssertion assertion )
		{
			assertionInspector.setTitle( "Assertions (" + getModelItem().getAssertionCount() + ")" );
		}

		public void assertionMoved( TestAssertion assertion, int ix, int offset )
		{
			assertionInspector.setTitle( "Assertions (" + getModelItem().getAssertionCount() + ")" );
		}
	}

	private void updateStatusIcon()
	{
		AssertionStatus status = amfRequestTestStep.getAssertionStatus();
		switch( status )
		{
		case FAILED :
		{
			assertionInspector.setIcon( UISupport.createImageIcon( "/failed_assertion.gif" ) );
			inspectorPanel.activate( assertionInspector );
			break;
		}
		case UNKNOWN :
		{
			assertionInspector.setIcon( UISupport.createImageIcon( "/unknown_assertion.gif" ) );
			break;
		}
		case VALID :
		{
			assertionInspector.setIcon( UISupport.createImageIcon( "/valid_assertion.gif" ) );
			inspectorPanel.deactivate();
			break;
		}
		}
	}
}