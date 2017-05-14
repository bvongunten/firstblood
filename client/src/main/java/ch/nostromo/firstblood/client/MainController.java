package ch.nostromo.firstblood.client;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.management.JMX;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import ch.nostromo.firstblood.agent.FirstBloodAgentControllerMBean;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * JavaFX Controller for the main window
 * 
 * @author Bernhard von Gunten <bvg@nostromo.ch>
 *
 */
public class MainController implements Initializable {

    public static final String APP_PREFS = "FirstBloodClient";
    public static final String APP_PREFS_AGENT_FILE = "AgentFile";
    public static final String APP_PREFS_CLASS_FILTER = "ClassFilter";
    public static final String APP_PREFS_CLASS_NAME = "ClassName";
    public static final String APP_PREFS_METHOD_NAME = "MethodName";
    public static final String APP_PREFS_METHOD_BODY = "MethodBody";
        
    @FXML
    private TextField txtAgentFile;
    
    @FXML
    private TextField txtClassFilter;

    @FXML
    private TextField txtClassName;
    
    @FXML
    private TextField txtMethodName;
    
    @FXML
    private TextArea txtMethodCode;
    
    @FXML
    private Button btnAttach;
    
    @FXML
    private Button btnSelectAgent;
    
    @FXML
    private Button btnListClasses;
    
    @FXML
    private Button btnGetSourceCode;
    
    @FXML
    private Button btnSetCode;
    
    @FXML
    private Button btnDetach;

    /**
     * Currently loaded VirtualMachine
     */
    private VirtualMachine currentVm;
    
    /**
     * Currently attached MBean
     */
    private FirstBloodAgentControllerMBean mbean;

    /**
     * Preferences
     */
    private Preferences preferences;


    /**
     * Load preferences 
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
        preferences = Preferences.userRoot().node(APP_PREFS);
        
        txtAgentFile.setText(preferences.get(APP_PREFS_AGENT_FILE, ""));
        txtClassFilter.setText(preferences.get(APP_PREFS_CLASS_FILTER, ""));
        txtClassName.setText(preferences.get(APP_PREFS_CLASS_NAME, ""));
        txtMethodName.setText(preferences.get(APP_PREFS_METHOD_NAME, ""));
        txtMethodCode.setText(preferences.get(APP_PREFS_METHOD_BODY, ""));
        
        setButtonsState(true);
    }

    // Set Buttons state
    private void setButtonsState(boolean dettached) {
        this.btnSelectAgent.setDisable(!dettached);
        this.btnAttach.setDisable(!dettached);

        this.btnDetach.setDisable(dettached);
        this.btnGetSourceCode.setDisable(dettached);
        this.btnListClasses.setDisable(dettached);
        this.btnSetCode.setDisable(dettached);
    }
    
    /**
     * Show file open dialog to select the agent jar file.
     * 
     * @param event
     */
    @FXML
    void actionSelectAgent(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Jar Files", "*.jar"));
        
        File agentFile = new File (txtAgentFile.getText());
        if (agentFile.exists()) {
            fileChooser.setInitialDirectory(agentFile.getParentFile());
        } 
        
        File selectedFile = fileChooser.showOpenDialog(txtAgentFile.getScene().getWindow());
        if (selectedFile != null) {
            txtAgentFile.setText(selectedFile.getAbsolutePath());
            preferences.put(APP_PREFS_AGENT_FILE, selectedFile.getAbsolutePath());
        }
    }

    /**
     * Show choice dialog with current running VMs
     * 
     * @param event
     */
    @FXML
    void actionAttach(ActionEvent event) {
        ChoiceDialog<VirtualMachineDescriptor> dialog = new ChoiceDialog<>(null, VirtualMachine.list());
        dialog.setTitle("Virtual Machine List");
        dialog.setHeaderText("Select the VM");
        dialog.setContentText("VMs:");

        Optional<VirtualMachineDescriptor> result = dialog.showAndWait();
        if (result.isPresent()) {
            fireUp(txtAgentFile.getText(), result.get().id());
            setButtonsState(false);
        }
    }

    /**
     * Fire up the remote control of a VM including loading the agent and connecting to the remote MBean.
     * 
     * @param agentFile
     * @param vmId
     */
    public void fireUp(String agentFile, String vmId) {
        
        // Attach VM
        try {
            currentVm = VirtualMachine.attach(vmId);
        } catch (AttachNotSupportedException | IOException e) {
            throw new RuntimeException("Unable to attach the VM with Error: " + e.getMessage(), e);
        }

        // Load FB Agent
        try {
            currentVm.loadAgent(agentFile);
        } catch (AgentLoadException | AgentInitializationException | IOException e) {
            throw new RuntimeException("Unable load agent with Error: " + e.getMessage(), e);
        }

        // Load MBean
        try {

            String CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";
            Properties props = currentVm.getAgentProperties();
            String connectorAddress = props.getProperty(CONNECTOR_ADDRESS);
            if (connectorAddress == null) {
                String agent = currentVm.getSystemProperties().getProperty("java.home") + File.separator + "lib" + File.separator + "management-agent.jar";
                currentVm.loadAgent(agent);

                connectorAddress = currentVm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
                if (connectorAddress == null) {
                    throw new RuntimeException("Unable to get connector address for the MBean");
                }
            }

            JMXServiceURL url = new JMXServiceURL(connectorAddress);
            JMXConnector jmxConnector = JMXConnectorFactory.connect(url);
            MBeanServerConnection mbeanConn = jmxConnector.getMBeanServerConnection();
            Set<ObjectName> beanSet = mbeanConn.queryNames(null, null);
            for (ObjectName on : beanSet) {
                if (FirstBloodAgentControllerMBean.MBEAN_OBJECT_NAMAE.equals(on.getCanonicalName())) {
                    mbean = JMX.newMBeanProxy(mbeanConn, on, FirstBloodAgentControllerMBean.class, true);
                }
            }
            
            if (mbean == null) {
                throw new RuntimeException("Unable to get the MBean from VM");
            }

            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Virtual Machine");
            alert.setContentText("Agent & MBean successfully loaded.");
            alert.showAndWait();
            
        } catch (Exception e) {
            throw new RuntimeException("Unable get MBean with error: " + e.getMessage(), e);
        }

    }


    /**
     * Show a list of all loaded classes in the remote VM.
     * 
     * @param event
     * @throws MBeanException
     */
    @FXML
    void actionListClasses(ActionEvent event) throws MBeanException {
       preferences.put(APP_PREFS_CLASS_FILTER, txtClassFilter.getText());

       List<String> classes = mbean.getLoadedClasses(txtClassFilter.getText());
       StringBuffer text = new StringBuffer();
       for (String clazz : classes) {
           text.append(clazz + System.lineSeparator());
       }

       FirstBloodClient.showTextAreaDialog(AlertType.INFORMATION, "Loaded classes", "List", text.toString());

    }
    
    /**
     * Show the source code of a given class in the remote VM
     * 
     * @param event
     * @throws MBeanException
     */
    @FXML
    void actionGetSourceCode(ActionEvent event) throws MBeanException {
        preferences.put(APP_PREFS_CLASS_NAME, txtClassName.getText());

        String code = mbean.getClassSource(txtClassName.getText());
        FirstBloodClient.showTextAreaDialog(AlertType.INFORMATION, "Decompiler", "Code", code);
    }


    /**
     * Overwrite the code of a given class & method in the remote VM
     * 
     * @param event
     * @throws MBeanException
     */
    @FXML
    void actionSetCode(ActionEvent event) throws MBeanException {
       preferences.put(APP_PREFS_METHOD_NAME, txtMethodName.getText());
       preferences.put(APP_PREFS_METHOD_BODY, txtMethodCode.getText());

       mbean.changeMethodBody(txtClassName.getText(), txtMethodName.getText(), txtMethodCode.getText());
       
       Alert alert = new Alert(AlertType.INFORMATION);
       alert.setTitle("Virtual Machine");
       alert.setContentText("Code successfully injected.");
       alert.showAndWait();
    }
    
    /**
     * Detach the MBean & VirtualMachine
     * @param event
     * @throws IOException
     * @throws MBeanException 
     */
    @FXML
    void actionDetach(ActionEvent event) throws IOException, MBeanException {
        mbean.unregister();
        currentVm.detach();
        
        mbean = null;
        currentVm = null;
        
        setButtonsState(true);
    }


}
