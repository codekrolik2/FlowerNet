package com.flower.socksui.forms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.flower.conntrack.ConnectionListenerAndFilter;
import com.flower.sockschain.config.ProxyChainProvider;
import com.flower.sockschain.config.SocksNode;
import com.flower.sockschain.server.SocksChainServerConnectHandler;
import com.flower.socksserver.SocksServer;
import com.flower.socksui.MainApp;
import com.flower.socksui.ModalWindow;
import com.flower.socksui.chainconf.ChainConfiguration;
import com.flower.socksui.chainconf.ImmutableChainConfiguration;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class ServerForm extends AnchorPane implements Refreshable, ProxyChainProvider {
    final static Logger LOGGER = LoggerFactory.getLogger(ServerForm.class);

    final ReentrantLock startLock;
    final MainApp mainApp;
    boolean isStarted;

    @Nullable @FXML Button startButton;
    @Nullable @FXML Button stopButton;
    @Nullable @FXML TextField portTextField;
    @Nullable @FXML TextField serverNumberTextField;
    @Nullable @FXML CheckBox uniqueServersCheckBox;

    @Nullable Stage stage;
    SocksServer server;

    @FXML @Nullable TableView<FXSocksNode> knownServersTable;
    final ObservableList<FXSocksNode> knownServers;
    @FXML @Nullable TableView<FXSocksNode> socksChainTable;
    final ObservableList<FXSocksNode> socksChain;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public ServerForm(MainApp mainApp) {
        this.mainApp = mainApp;
        startLock = new ReentrantLock();
        isStarted = false;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ServerForm.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        server = new SocksServer(false,
                () -> new SocksChainServerConnectHandler(this));

        knownServers = FXCollections.observableArrayList();
        checkNotNull(knownServersTable).itemsProperty().set(knownServers);

        socksChain = FXCollections.observableArrayList();
        checkNotNull(socksChainTable).itemsProperty().set(socksChain);
    }

    public void addConnectionListenerAndFilter(ConnectionListenerAndFilter connectionListenerAndFilter) {
        server.addConnectionListenerAndFilter(connectionListenerAndFilter);
    }

    public void startServer() throws InterruptedException {
        try {
            startLock.lock();
            if (!isStarted) {
                String portStr = checkNotNull(portTextField).textProperty().get();
                int port;
                try {
                    port = Integer.parseInt(portStr);
                } catch (Exception e) {
                    port = -1;
                }

                if (port <= 0) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Please specify integer port > 0 (specified:" + portStr + ")", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }

                checkNotNull(server).startServer(port, null).sync();
                mainApp.setStatusText("Server started on port " + port);
                isStarted = true;
            }
            checkNotNull(portTextField).setDisable(true);
            checkNotNull(startButton).setDisable(true);
            checkNotNull(stopButton).setDisable(false);
        } finally {
            startLock.unlock();
        }
    }

    public void stopServer() {
        try {
            startLock.lock();
            if (isStarted) {
                checkNotNull(server).shutdownServer();
                mainApp.setStatusText("Server not running");
                isStarted = false;
            }
            checkNotNull(portTextField).setDisable(false);
            checkNotNull(startButton).setDisable(false);
            checkNotNull(stopButton).setDisable(true);
        } finally {
            startLock.unlock();
        }
    }

    void addKnownServer(SocksNode socksNode) {
        Platform.runLater(() -> {
            knownServers.add(new FXSocksNode(socksNode));
            checkNotNull(knownServersTable).refresh();
        });
    }

    public void newServer() {
        try {
            SocksNodeAddDialog socksNodeAddDialog = new SocksNodeAddDialog(null);
            Stage workspaceStage = ModalWindow.showModal(checkNotNull(mainApp.getMainStage()),
                    stage -> { socksNodeAddDialog.setStage(stage); return socksNodeAddDialog; },
                    "Add new server");

            workspaceStage.setOnHidden(
                    ev -> {
                        try {
                            SocksNode socksNode = socksNodeAddDialog.getSocksNode();
                            if (socksNode != null) {
                                addKnownServer(socksNode);
                                refreshContent();
                            }
                        } catch (Exception e) {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Error adding known server: " + e, ButtonType.OK);
                            LOGGER.error("Error adding known server: ", e);
                            alert.showAndWait();
                        }
                    }
            );
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error adding known server: " + e, ButtonType.OK);
            LOGGER.error("Error adding known server: ", e);
            alert.showAndWait();
        }
    }

    public void editServer() {
        try {
            FXSocksNode selectedSocksNode = checkNotNull(knownServersTable).getSelectionModel().getSelectedItem();
            int selectedIndex = checkNotNull(knownServersTable).getSelectionModel().getSelectedIndex();
            if (selectedSocksNode != null) {
                SocksNodeAddDialog socksNodeAddDialog = new SocksNodeAddDialog(selectedSocksNode);
                Stage workspaceStage = ModalWindow.showModal(checkNotNull(mainApp.getMainStage()),
                        stage -> {
                            socksNodeAddDialog.setStage(stage);
                            return socksNodeAddDialog;
                        },
                        "Edit server");

                workspaceStage.setOnHidden(
                        ev -> {
                            try {
                                SocksNode socksNode = socksNodeAddDialog.getSocksNode();
                                if (socksNode != null) {
                                    knownServers.remove(selectedIndex);
                                    knownServers.add(selectedIndex, new FXSocksNode(socksNode));
                                    checkNotNull(knownServersTable).refresh();
                                    refreshContent();
                                }
                            } catch (Exception e) {
                                Alert alert = new Alert(Alert.AlertType.ERROR, "Error editing known server: " + e, ButtonType.OK);
                                LOGGER.error("Error editing known server: ", e);
                                alert.showAndWait();
                            }
                        }
                );
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error adding known server: " + e, ButtonType.OK);
            LOGGER.error("Error adding known server: ", e);
            alert.showAndWait();
        }
    }

    public void deleteServer() {
        try {
            FXSocksNode selectedSocksNode = checkNotNull(knownServersTable).getSelectionModel().getSelectedItem();
            if (selectedSocksNode != null) {
                knownServers.remove(selectedSocksNode);
                checkNotNull(knownServersTable).refresh();
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error deleting known server: " + e, ButtonType.OK);
            LOGGER.error("Error deleting known server: ", e);
            alert.showAndWait();
        }
    }

    public void addServerToChain() {
        try {
            FXSocksNode selectedSocksNode = checkNotNull(knownServersTable).getSelectionModel().getSelectedItem();
            if (selectedSocksNode != null) {
                socksChain.add(selectedSocksNode);
                checkNotNull(socksChainTable).refresh();
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error deleting known server: " + e, ButtonType.OK);
            LOGGER.error("Error deleting known server: ", e);
            alert.showAndWait();
        }
    }

    public void removeServerFromChain() {
        try {
            FXSocksNode selectedSocksNode = checkNotNull(socksChainTable).getSelectionModel().getSelectedItem();
            if (selectedSocksNode != null) {
                socksChain.remove(selectedSocksNode);
                checkNotNull(socksChainTable).refresh();
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error removing server from chain: " + e, ButtonType.OK);
            LOGGER.error("Error removing server from chain: ", e);
            alert.showAndWait();
        }
    }

    public void clearServerChain() {
        try {
            socksChain.clear();
            checkNotNull(socksChainTable).refresh();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error clearing server chain: " + e, ButtonType.OK);
            LOGGER.error("Error clearing server chain: ", e);
            alert.showAndWait();
        }
    }

    public void moveServerUpInChain() {
        try {
            int selectedIndex = checkNotNull(socksChainTable).getSelectionModel().getSelectedIndex();
            if (selectedIndex > 0) {
                FXSocksNode selectedSocksNode = socksChain.remove(selectedIndex);
                socksChain.add(selectedIndex-1, selectedSocksNode);
                checkNotNull(socksChainTable).getSelectionModel().select(selectedIndex-1);
                checkNotNull(socksChainTable).refresh();
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error moving server up in chain: " + e, ButtonType.OK);
            LOGGER.error("Error moving server up in chain: ", e);
            alert.showAndWait();
        }
    }

    public void moveServerDownInChain() {
        try {
            int selectedIndex = checkNotNull(socksChainTable).getSelectionModel().getSelectedIndex();
            if (selectedIndex < socksChain.size() - 1) {
                FXSocksNode selectedSocksNode = socksChain.remove(selectedIndex);
                socksChain.add(selectedIndex+1, selectedSocksNode);
                checkNotNull(socksChainTable).getSelectionModel().select(selectedIndex+1);
                checkNotNull(socksChainTable).refresh();
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error moving server down in chain: " + e, ButtonType.OK);
            LOGGER.error("Error moving server down in chain: ", e);
            alert.showAndWait();
        }
    }

    @Override
    public void refreshContent() {
        // TODO:
    }

    public void notImplemented() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Function not implemented yet", ButtonType.OK);
        alert.showAndWait();
    }

    public void addRandom() {
        String serverCountStr = checkNotNull(serverNumberTextField).textProperty().get();
        int serverCount;
        try {
            serverCount = Integer.parseInt(serverCountStr);
        } catch (Exception e) {
            serverCount = -1;
        }

        if (serverCount <= 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Please specify server count > 0 (specified:" + serverCountStr + ")", ButtonType.OK);
            alert.showAndWait();
        }

        boolean useUnique = checkNotNull(uniqueServersCheckBox).selectedProperty().get();
        if (useUnique) {
            Set<FXSocksNode> currentChain = new HashSet<>(socksChain);
            List<FXSocksNode> unusedServersListSnapshot = knownServers.stream().filter(server -> !currentChain.contains(server)).collect(Collectors.toList());
            Collections.shuffle(unusedServersListSnapshot);
            if (serverCount > unusedServersListSnapshot.size()) { serverCount = unusedServersListSnapshot.size(); }
            for (int i = 0; i < serverCount; i++) {
                socksChain.add(unusedServersListSnapshot.get(i));
            }
        } else {
            List<FXSocksNode> knownServersListSnapshot = knownServers.stream().toList();
            for (int i = 0; i < serverCount; i++) {
                int serverIndex = (int)(Math.random() * knownServersListSnapshot.size());
                socksChain.add(knownServersListSnapshot.get(serverIndex));
            }
        }
        checkNotNull(socksChainTable).refresh();
    }

    @Override
    public List<SocksNode> getProxyChain() {
        return socksChain.stream().map(fx -> (SocksNode)fx).toList();
    }

    public void saveConfigToFile() throws IOException {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Proxy chain config file (*.chn)", "*.chn"));
            fileChooser.setTitle("Save Config file");
            File configFile = fileChooser.showSaveDialog(checkNotNull(stage));

            if (configFile != null) {
                if (!configFile.getName().endsWith(".chn")) {
                    configFile = new File(configFile.getPath()  + ".chn");
                }
                ChainConfiguration config = ImmutableChainConfiguration.builder()
                        .knownProxyServers(knownServers.stream().map(f -> f.node).toList())
                        .proxyChain(socksChain.stream().map(f -> f.node).toList())
                        .build();

                ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
                        .registerModule(new GuavaModule());
                mapper.writeValue(configFile, config);

                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Config file saved to : " + configFile.getPath(), ButtonType.OK);
                LOGGER.error("Config file saved to : " + configFile.getPath());
                alert.showAndWait();
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error saving config file: " + e, ButtonType.OK);
            LOGGER.error("Error saving config file: ", e);
            alert.showAndWait();
        }
    }

    public void loadConfigFromFile() throws IOException {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Proxy chain config file (*.chn)", "*.chn"));
            fileChooser.setTitle("Open Config file");

            File configFile = fileChooser.showOpenDialog(checkNotNull(stage));

            if (configFile != null) {
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
                        .registerModule(new GuavaModule());

                ChainConfiguration chainConfiguration = mapper.readValue(configFile, ChainConfiguration.class);

                knownServers.clear();
                knownServers.addAll(chainConfiguration.knownProxyServers().stream().map(FXSocksNode::new).toList());
                checkNotNull(knownServersTable).itemsProperty().set(knownServers);

                socksChain.clear();
                socksChain.addAll(chainConfiguration.proxyChain().stream().map(FXSocksNode::new).toList());
                checkNotNull(socksChainTable).itemsProperty().set(socksChain);
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error loading config file: " + e, ButtonType.OK);
            LOGGER.error("Error loading config file: ", e);
            alert.showAndWait();
        }
    }
}