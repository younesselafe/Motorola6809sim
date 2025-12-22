package com.sim6809.ui;

import com.sim6809.model.Bus;
import com.sim6809.model.Cpu6809;
import com.sim6809.model.Asm;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class MainApp extends Application {

    private Bus bus;
    private Cpu6809 cpu;
    private boolean isRunning = false;
    private AnimationTimer cpuLoop;
    private Stage primaryStage;

    // Composants UI
    private TextArea codeEditor, consoleArea;
    private ListView<String> varsView, contextView;
    private ObservableList<String> varsData, contextData;
    
    // Labels Registres
    private Label valA, valB, valX, valY, valS, valU, valPC, valDP;
    
    // Drapeaux (Flags)
    private Label[] flagLeds = new Label[8]; 
    private Label[] flagBits = new Label[8]; 
    
    private ToggleButton btnHalt;
    private Label lblStatus;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        bus = new Bus();
        cpu = new Cpu6809(bus);
        
        BorderPane root = new BorderPane();
        root.setStyle("-fx-base: #1e1e1e; -fx-control-inner-background: #1e1e1e; -fx-background-color: #121212;");

        VBox topContainer = new VBox();
        topContainer.getChildren().addAll(createMenuBar(), createToolbar());
        root.setTop(topContainer);

        SplitPane mainSplit = new SplitPane();
        mainSplit.setStyle("-fx-background-color: #121212;");
        
        VBox leftPane = new VBox(10); 
        leftPane.setPadding(new Insets(10));
        VBox editorBox = new VBox(5, header("SOURCE CODE"), codeEditor = new TextArea(getDefaultProgram()));
        codeEditor.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 14px; -fx-text-fill: #e0e0e0;");
        VBox.setVgrow(editorBox, Priority.ALWAYS); 
        VBox.setVgrow(codeEditor, Priority.ALWAYS);
        leftPane.getChildren().addAll(editorBox, createInterruptPanel());

        SplitPane centerSplitVertical = new SplitPane();
        centerSplitVertical.setOrientation(Orientation.VERTICAL);
        centerSplitVertical.setPadding(new Insets(10));
        centerSplitVertical.setStyle("-fx-background-color: transparent;");

        SplitPane memorySplit = new SplitPane(); 
        memorySplit.setOrientation(Orientation.HORIZONTAL);
        
        varsData = FXCollections.observableArrayList(); 
        varsView = new ListView<>(varsData); 
        varsView.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 13px;");
        VBox varBox = new VBox(5, header("VARS"), varsView);
        VBox.setVgrow(varsView, Priority.ALWAYS);
        
        contextData = FXCollections.observableArrayList(); 
        contextView = new ListView<>(contextData); 
        contextView.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 13px;");
        VBox ctxBox = new VBox(5, header("CONTEXTE"), contextView);
        VBox.setVgrow(contextView, Priority.ALWAYS);
        
        memorySplit.getItems().addAll(varBox, ctxBox); 
        memorySplit.setDividerPositions(0.3); 
        
        consoleArea = new TextArea(); 
        consoleArea.setEditable(false); 
        consoleArea.setStyle("-fx-text-fill: #00E676; -fx-control-inner-background: #000000; -fx-font-family: 'Consolas'; -fx-border-color: #444;");
        
        VBox consoleBox = new VBox(5, header("CONSOLE"), consoleArea); 
        VBox.setVgrow(consoleArea, Priority.ALWAYS); 
        
        centerSplitVertical.getItems().addAll(memorySplit, consoleBox);
        centerSplitVertical.setDividerPositions(0.6);

        ScrollPane rightPane = createVisualUALPanel();

        mainSplit.getItems().addAll(leftPane, centerSplitVertical, rightPane);
        mainSplit.setDividerPositions(0.25, 0.70);
        
        root.setCenter(mainSplit);
        root.setBottom(createStatusBar());

        setupCpuLoop();
        bus.addListener((a,v) -> refreshMemoryView());

        Scene scene = new Scene(root, 1200, 720);
        primaryStage.setTitle("Motorola 6809 Studio - Ultimate v32 (Stacks S & U)");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        refreshUI();
    }

    private ScrollPane createVisualUALPanel() {
        VBox p = new VBox(20);
        p.setPadding(new Insets(20));
        p.setStyle("-fx-background-color: #181818;");
        p.setAlignment(Pos.TOP_CENTER);

        String colBleuCiel = "#00B0FF"; 
        String colBleuStd  = "#2979FF"; 
        String colValue    = "#00E676"; 

        HBox abBox = new HBox(20); 
        abBox.setAlignment(Pos.CENTER);
        valA = createRegisterBox("ACC A", colBleuCiel, colValue); 
        valB = createRegisterBox("ACC B", colBleuCiel, colValue);
        abBox.getChildren().addAll(valA, valB);

        Pane ualShape = drawUALShape();
        Label arrow = new Label("⬇ RESULTAT & FLAGS ⬇");
        arrow.setTextFill(Color.GRAY); 
        arrow.setFont(Font.font("Arial", 10));

        VBox ccBox = new VBox(15);
        ccBox.setAlignment(Pos.CENTER);
        
        Label lblCC = new Label("CONDITION CODE (CC)");
        lblCC.setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold; -fx-font-family: 'Arial'; -fx-font-size: 14px;");
        
        HBox flagsLeds = createFlagsDisplay();
        ccBox.getChildren().addAll(lblCC, flagsLeds);
        ccBox.setStyle("-fx-background-color: #222; -fx-padding: 25; -fx-background-radius: 8; -fx-border-color: #555; -fx-border-radius: 8;");

        VBox ptrBox = new VBox(15);
        ptrBox.getChildren().add(header("POINTEURS & INDEX"));
        
        valX = createLineReg("INDEX X", colBleuStd, colValue);
        valY = createLineReg("INDEX Y", colBleuStd, colValue);
        valS = createLineReg("PILE SYSTEM (S)", colBleuStd, colValue);
        valU = createLineReg("PILE USER (U)", colBleuStd, colValue);
        valPC = createLineReg("PROG COUNTER (PC)", colBleuStd, colValue);
        valDP = createLineReg("DIRECT PAGE (DP)", colBleuStd, colValue);
        
        ptrBox.getChildren().addAll(valX, valY, valS, valU, valPC, valDP);

        p.getChildren().addAll(abBox, ualShape, arrow, ccBox, new Separator(), ptrBox);
        
        ScrollPane sp = new ScrollPane(p);
        sp.setFitToWidth(true); 
        sp.setFitToHeight(true);
        sp.setStyle("-fx-background: #181818; -fx-border-color: transparent;");
        return sp;
    }

    private HBox createFlagsDisplay() {
        HBox box = new HBox(15); 
        box.setAlignment(Pos.CENTER);
        String[] names = {"E", "F", "H", "I", "N", "Z", "V", "C"};
        
        for(int i=0; i<8; i++) {
            VBox col = new VBox(6); 
            col.setAlignment(Pos.CENTER);
            
            Label led = new Label(names[i]);
            led.setFont(Font.font("Arial", FontWeight.BOLD, 13));
            led.setPrefSize(32, 32); 
            led.setAlignment(Pos.CENTER);
            led.setStyle("-fx-background-color: #333; -fx-text-fill: #555; -fx-background-radius: 16;");
            flagLeds[i] = led;
            
            Label bit = new Label("0");
            bit.setStyle("-fx-text-fill: #FFFF00; -fx-font-family: 'Monospaced'; -fx-font-weight: bold; -fx-font-size: 16px;");
            flagBits[i] = bit;
            
            col.getChildren().addAll(led, bit);
            box.getChildren().add(col);
        }
        return box;
    }

    private Pane drawUALShape() {
        Polygon ual = new Polygon();
        ual.getPoints().addAll(new Double[]{ 
            0.0, 0.0, 
            60.0, 80.0, 
            120.0, 0.0, 
            95.0, 0.0, 
            60.0, 50.0, 
            25.0, 0.0 
        });
        
        ual.setFill(Color.TRANSPARENT); 
        ual.setStroke(Color.web("#FFD700")); 
        ual.setStrokeWidth(3);
        
        Label lbl = new Label("UAL"); 
        lbl.setTextFill(Color.web("#FFD700")); 
        lbl.setFont(Font.font("Impact", 20)); 
        lbl.setLayoutX(43); 
        lbl.setLayoutY(10);
        
        Pane p = new Pane(ual, lbl); 
        p.setMaxSize(120, 80); 
        p.setMinSize(120, 80); 
        return p;
    }

    private Label createRegisterBox(String title, String titleColor, String valueColor) {
        Label val = new Label("00"); 
        val.setAlignment(Pos.CENTER);
        
        Label tit = new Label(title); 
        tit.setStyle("-fx-text-fill: " + titleColor + "; -fx-font-family: 'Arial'; -fx-font-weight: bold; -fx-font-size: 10px;");
        
        val.setGraphic(tit); 
        val.setContentDisplay(ContentDisplay.TOP);
        
        val.setStyle("-fx-text-fill: " + valueColor + ";" +
                     "-fx-font-family: 'Monospaced'; -fx-font-weight: bold; -fx-font-size: 30px;" +
                     "-fx-border-color: " + titleColor + "; -fx-border-width: 2; -fx-border-radius: 5; " +
                     "-fx-padding: 10 15 10 15; -fx-background-color: #252526;");
        val.setMinWidth(100); 
        return val;
    }

    private Label createLineReg(String title, String titleColor, String valueColor) {
        Label val = new Label("0000"); 
        val.setStyle("-fx-text-fill: " + valueColor + "; -fx-font-family: 'Monospaced'; -fx-font-weight: bold; -fx-font-size: 24px;");
        
        HBox box = new HBox(); 
        box.setAlignment(Pos.CENTER_LEFT); 
        box.setPadding(new Insets(12, 15, 12, 15));
        box.setStyle("-fx-background-color: #222; -fx-background-radius: 4; -fx-border-color: #333; -fx-border-radius: 4;");
        
        Label tit = new Label(title); 
        tit.setStyle("-fx-text-fill: " + titleColor + "; -fx-font-family: 'Arial'; -fx-font-size: 13px;");
        tit.setPrefWidth(140);
        
        box.getChildren().addAll(tit, val);
        
        Label wrapper = new Label(); 
        wrapper.setGraphic(box); 
        wrapper.setContentDisplay(ContentDisplay.GRAPHIC_ONLY); 
        wrapper.setUserData(val);
        return wrapper;
    }
    
    private void updateReg(Label wrapper, String txt) {
        if(wrapper.getUserData() instanceof Label) ((Label)wrapper.getUserData()).setText(txt); 
        else wrapper.setText(txt);
    }

    private VBox createInterruptPanel() {
        VBox box = new VBox(8); 
        box.setStyle("-fx-background-color: #252526; -fx-border-color: #444; -fx-border-radius: 5; -fx-padding: 8;");
        Label title = new Label("CONTRÔLE MATÉRIEL"); 
        title.setTextFill(Color.GRAY); 
        title.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        
        HBox row = new HBox(5);
        btnHalt = new ToggleButton("HALT"); 
        btnHalt.setStyle("-fx-base: #D32F2F; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px;"); 
        btnHalt.setOnAction(e -> cpu.lineHALT = btnHalt.isSelected());
        
        // --- MODIFICATION ICI : Connexion réelle aux lignes du CPU ---
        Button btnIrq = new Button("IRQ"); 
        btnIrq.setOnAction(e -> { 
            cpu.lineIRQ = true; 
            log(">> SIGNAL IRQ ENVOYÉ"); 
        });

        Button btnFirq = new Button("FIRQ"); 
        btnFirq.setOnAction(e -> { 
            cpu.lineFIRQ = true; 
            log(">> SIGNAL FIRQ ENVOYÉ"); 
        });

        Button btnNmi = new Button("NMI"); 
        btnNmi.setOnAction(e -> { 
            cpu.lineNMI = true; 
            log(">> SIGNAL NMI ENVOYÉ"); 
        });
        // -------------------------------------------------------------
        
        String style = "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8 3 8;";
        btnIrq.setStyle(style + "-fx-base: #FF9800;"); 
        btnFirq.setStyle(style + "-fx-base: #9C27B0; -fx-text-fill: white;"); 
        btnNmi.setStyle(style + "-fx-base: #E91E63; -fx-text-fill: white;");
        
        row.getChildren().addAll(btnHalt, new Separator(Orientation.VERTICAL), btnIrq, btnFirq, btnNmi); 
        row.setAlignment(Pos.CENTER_LEFT);
        
        box.getChildren().addAll(title, row); 
        return box;
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar(); 
        menuBar.setStyle("-fx-background-color: #2D2D30; -fx-text-fill: white;");
        
        Menu fileMenu = new Menu("Fichier"); 
        MenuItem exitItem = new MenuItem("Quitter");
        exitItem.setAccelerator(KeyCombination.keyCombination("Ctrl+Q")); 
        exitItem.setOnAction(e -> { Platform.exit(); System.exit(0); });
        fileMenu.getItems().add(exitItem);
        
        Menu viewMenu = new Menu("Affichage"); 
        MenuItem fsItem = new MenuItem("Plein Écran (On/Off)");
        fsItem.setAccelerator(KeyCombination.keyCombination("F11")); 
        fsItem.setOnAction(e -> primaryStage.setFullScreen(!primaryStage.isFullScreen()));
        viewMenu.getItems().add(fsItem);
        
        Menu helpMenu = new Menu("Aide"); 
        MenuItem aboutItem = new MenuItem("À propos"); 
        aboutItem.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("À propos");
            alert.setHeaderText("Simulateur Motorola 6809");
            alert.setContentText("Version 32 (Stacks S & U)");
            alert.show();
        }); 
        helpMenu.getItems().add(aboutItem);
        
        menuBar.getMenus().addAll(fileMenu, viewMenu, helpMenu); 
        return menuBar;
    }

    private HBox createToolbar() {
        HBox tb = new HBox(10); 
        tb.setPadding(new Insets(10));
        tb.setStyle("-fx-background-color: #252526; -fx-border-color: #333; -fx-border-width: 0 0 1 0;");
        
        Button btnAssemble = new Button("ASSEMBLER"); 
        btnAssemble.setStyle("-fx-background-color: #0D47A1; -fx-text-fill: white; -fx-font-weight: bold;"); 
        btnAssemble.setOnAction(e -> assembleAndLoad());
        
        Button btnStep = new Button("PAS À PAS"); 
        btnStep.setStyle("-fx-background-color: #424242; -fx-text-fill: white;"); 
        btnStep.setOnAction(e -> step());
        
        Button btnRun = new Button("RUN"); 
        btnRun.setStyle("-fx-background-color: #1B5E20; -fx-text-fill: white;"); 
        btnRun.setOnAction(e -> { isRunning=true; cpuLoop.start(); });
        
        Button btnStop = new Button("STOP"); 
        btnStop.setStyle("-fx-background-color: #B71C1C; -fx-text-fill: white;"); 
        btnStop.setOnAction(e -> { isRunning=false; cpuLoop.stop(); refreshUI(); });
        
        Button btnReset = new Button("RESET"); 
        btnReset.setOnAction(e -> { isRunning=false; cpuLoop.stop(); cpu.reset(); log("--- CPU RESET ---"); refreshUI(); });
        
        Button btnClearData = new Button("CLR MEM"); 
        btnClearData.setOnAction(e -> { 
            for(int i=0; i<0x100; i++) bus.write(i, 0); 
            for(int i=0x7F00; i<=0x7FFF; i++) bus.write(i, 0); 
            for(int i=0x6F00; i<=0x6FFF; i++) bus.write(i, 0); 
            log("--- DATA CLEARED ---"); 
            refreshUI(); 
        });
        
        tb.getChildren().addAll(btnAssemble, new Separator(), btnStep, btnRun, btnStop, new Separator(), btnReset, btnClearData); 
        return tb;
    }
    
    private HBox createStatusBar() { 
        HBox b = new HBox(); 
        b.setStyle("-fx-background-color: #007ACC; -fx-padding: 3;"); 
        lblStatus = new Label("PRÊT"); 
        lblStatus.setTextFill(Color.WHITE); 
        lblStatus.setFont(Font.font("Arial", 11)); 
        b.getChildren().add(lblStatus); 
        return b; 
    }

    private void setupCpuLoop() { 
        cpuLoop = new AnimationTimer() { 
            @Override public void handle(long now) { if(isRunning) { for(int i=0; i<10; i++) step(); } } 
        }; 
    }

    private void step() { 
        Asm.DisRes d = Asm.disassemble(bus, cpu.PC); 
        log(String.format("[%04X] %-10s | A:%02X B:%02X", cpu.PC, d.text, cpu.A, cpu.B)); 
        cpu.step(); 
        refreshUI(); 
    }

    private void refreshUI() {
        updateReg(valA, hex(cpu.A)); updateReg(valB, hex(cpu.B));
        updateReg(valX, hex16(cpu.X)); updateReg(valY, hex16(cpu.Y));
        updateReg(valS, hex16(cpu.S)); updateReg(valU, hex16(cpu.U));
        updateReg(valPC, hex16(cpu.PC)); updateReg(valDP, hex(cpu.DP));
        
        int cc = cpu.CC;
        for(int i=0; i<8; i++) {
            boolean on = ((cc >> (7-i)) & 1) == 1;
            flagLeds[i].setStyle(on ? 
                "-fx-background-color: #FFEB3B; -fx-text-fill: black; -fx-background-radius: 16; -fx-effect: dropshadow(three-pass-box, #FFEB3B, 5, 0.5, 0, 0);" : 
                "-fx-background-color: #333; -fx-text-fill: #555; -fx-background-radius: 16;");
                
            flagBits[i].setText(on ? "1" : "0");
            flagBits[i].setStyle("-fx-text-fill: " + (on ? "#FFFF00" : "#555555") + "; -fx-font-family: 'Monospaced'; -fx-font-weight: bold; -fx-font-size: 16px;");
        }
        
        refreshMemoryView();
    }
    
    private void refreshMemoryView() {
        varsData.clear(); 
        for(int i=0; i<=0x30; i++) { 
            int val = bus.read(i); 
            varsData.add(String.format("$%02X: %02X %s", i, val, (val!=0 ? "<-" : ""))); 
        }
        
        contextData.clear(); 
        contextData.add("=== CODE (PC) ===");
        int start = cpu.PC; 
        for(int i = start; i < start + 16; ) { 
            Asm.DisRes d = Asm.disassemble(bus, i); 
            String marker = (i == cpu.PC) ? ">>> " : "    "; 
            contextData.add(String.format("%s$%04X : %-12s | %s", marker, i, d.hexDump, d.text)); 
            i += d.bytesUsed; 
        }
        
        contextData.add(""); 
        contextData.add(String.format("=== PILE S ($%04X) ===", cpu.S)); 
        for(int i = cpu.S; i < cpu.S + 8; i++) 
            contextData.add(String.format("%s$%04X : %02X", (i==cpu.S?"[TOP] ":"      "), i, bus.read(i)));

        // --- AJOUT : PILE USER U ---
        contextData.add(""); 
        contextData.add(String.format("=== PILE U ($%04X) ===", cpu.U)); 
        for(int i = cpu.U; i < cpu.U + 8; i++) 
            contextData.add(String.format("%s$%04X : %02X", (i==cpu.U?"[TOP] ":"      "), i, bus.read(i)));
    }
    
    private void assembleAndLoad() { 
        isRunning = false; cpuLoop.stop(); consoleArea.clear(); 
        log("=== ASSEMBLAGE... ==="); 
        String result = Asm.assemble(codeEditor.getText(), bus, cpu); 
        log(result); 
        if(!result.contains("Error")) { cpu.reset(); log("=== PRÊT ==="); } 
        refreshUI(); 
    }

    private void log(String s) { consoleArea.appendText(s + "\n"); }
    private Label header(String txt) { Label l = new Label(txt); l.setTextFill(Color.web("#e0e0e0")); l.setFont(Font.font("System", FontWeight.BOLD, 12)); return l; }
    private String hex(int v) { return String.format("%02X", v); }
    private String hex16(int v) { return String.format("%04X", v); }
    private String getDefaultProgram() { return """
        ORG $8000       ; Début du programme

        ; --- INIT VECTEURS (IMPORTANT POUR IRQ/FIRQ/NMI) ---
        LDX #$9000
        STX $FFFC       ; NMI Vector -> $9000
        LDX #$9010
        STX $FFF6       ; FIRQ Vector -> $9010
        LDX #$9020
        STX $FFF8       ; IRQ Vector -> $9020

        ; --- INIT ---
        INIT:
            LDS #$7FFF  ; Stack S
            LDU #$6FFF  ; Stack U
            ; TFR A,DP  <-- SUPPRIMÉ POUR LA CLARTÉ DU TEST (DP reste à $00)
            
            LDA #$AA
            LDB #$55
            EXG A,B     ; A=$55, B=$AA
            STD $1000   ; Stocke D ($55AA) en $1000
            
            LDX #$1234
            LDY #$5678
            TFR X,D     ; D = $1234 (A=$12, B=$34)

        ; --- ARITHMETIQUE ---
        MATH16:
            LDD #$1000
            ADDD #$0234 ; D = $1234
            SUBD #$0034 ; D = $1200
            
            LDA #$10
            LDB #$10
            MUL         ; D = $10 * $10 = $0100 (256)

        ; --- INDEXATION ---
        INDEX:
            LDX #$2000
            LDA #$42
            STA ,X      ; Indexé simple
            INCA
            STA 1,X     ; Indexé offset 8 bits
            LDY #$0005
            STA 2,Y     ; Offset sur Y

        ; --- PILE ---
        STACK:
            LDA #$EE
            LDB #$FF
            ANDCC #$AF  ; DEMASQUER I et F (Autoriser Interruptions)
            PSHS D,CC   ; Empile A, B et CC sur S
            CLRA
            CLRB
            PULS CC,D   ; Dépile -> A=$EE, B=$FF
            BSR SOUS_PROG
            BRA LOGIC

        SOUS_PROG:
            LDA #$01
            RTS

        ; --- LOGIQUE ---
        LOGIC:
            LDA #$0F
            ORA #$F0
            ANDA #$AA
            EORA #$FF
            LDB #$01
            ASLB
            ASLB
            RORB
            LDA #$09
            ADDA #$01
            DAA

        ; --- BOUCLE FINALE ---
        FIN:
            INC $00     ; Compteur mémoire (regardez VARS $00)
            LDX $0000   ; Charge le compteur pour voir les flags bouger
            BRA FIN

        ; --- HANDLERS D'INTERRUPTION ---
        ORG $9000
        NMI_HANDLER:
            INC $01     ; Compteur NMI ($01)
            RTI

        ORG $9010
        FIRQ_HANDLER:
            INC $02     ; Compteur FIRQ ($02)
            RTI

        ORG $9020
        IRQ_HANDLER:
            INC $03     ; Compteur IRQ ($03)
            RTI
        """; }

    public static void main(String[] args) { launch(args); }
}
