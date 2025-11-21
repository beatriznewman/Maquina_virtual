import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;


public class Main extends JFrame {

    private EditorPanel editor;
    private JTextArea areaErro;
    private JLabel statusBar;
    private String textoFonte = "";
    private File arquivoAtual = null;   // <<< novo

    public Main() {
        super("Analisador - IDE Mini");

        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored){}

        setLayout(new BorderLayout());

        // ===========================
        // TOPO COM 3 BOTÕES
        // ===========================
        JTextField campoArquivo = new JTextField();
        campoArquivo.setEditable(false);

        JButton btnAbrir = new JButton("Abrir arquivo");
        JButton btnSalvar = new JButton("Salvar");
        JButton btnCompilar = new JButton("Compilar");

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        topo.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        topo.add(campoArquivo);
        topo.add(btnAbrir);
        topo.add(btnSalvar);
        topo.add(btnCompilar);
        add(topo, BorderLayout.NORTH);

        // ===========================
        // EDITOR
        // ===========================
        editor = new EditorPanel();
        add(editor, BorderLayout.CENTER);

        // ===========================
        // ÁREA DE ERROS + STATUS
        // ===========================
        JPanel bottom = new JPanel(new BorderLayout());
        areaErro = new JTextArea();
        areaErro.setForeground(Color.RED);
        areaErro.setEditable(false);
        areaErro.setBorder(BorderFactory.createTitledBorder("Erros"));
        areaErro.setRows(2);
        JScrollPane spErro = new JScrollPane(areaErro);
        spErro.setPreferredSize(new Dimension(10, 60));

        statusBar = new JLabel(" Pronto ");
        statusBar.setBorder(BorderFactory.createEmptyBorder(4,6,4,6));
        statusBar.setOpaque(true);

        bottom.add(spErro, BorderLayout.CENTER);
        bottom.add(statusBar, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        areaErro.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { irParaLinhaDoErro(); }
        });

        // ABRIR
        btnAbrir.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                arquivoAtual = chooser.getSelectedFile();
                campoArquivo.setText(arquivoAtual.getAbsolutePath());
                carregarArquivo(arquivoAtual);
            }
        });

        // SALVAR (NOVO)
        btnSalvar.addActionListener(e -> {
            if (arquivoAtual == null) {
                JOptionPane.showMessageDialog(this, "Nenhum arquivo carregado.");
                return;
            }
            salvarArquivo(arquivoAtual);
        });

        // COMPILAR (RECOMPILAR)
        btnCompilar.addActionListener(e -> {
            if (arquivoAtual == null) {
                JOptionPane.showMessageDialog(this, "Nenhum arquivo carregado.");
                return;
            }
            salvarArquivo(arquivoAtual);       // <<< para garantir que o analisador leia a versão atual
            executarAnalise(arquivoAtual.getAbsolutePath());
        });

        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    // ===========================
    // FUNÇÃO SALVAR (NOVA)
    // ===========================
    private void salvarArquivo(File f) {
        try (FileWriter fw = new FileWriter(f, false)) {
            fw.write(editor.txt.getText());
            statusBar.setText(" Alterações salvas.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar arquivo.");
        }
    }

    // ===========================
    // RESTANTE IGUAL
    // ===========================
    private void carregarArquivo(File f) {
        try {
            textoFonte = Files.readString(f.toPath());
            editor.txt.setText(textoFonte);
            statusBar.setText(" " + f.getAbsolutePath());
            areaErro.setText("");
        } catch (Exception ex) {
            areaErro.setText("Erro ao carregar arquivo.");
        }
    }

    private void executarAnalise(String caminho) {
        areaErro.setForeground(Color.RED);
        areaErro.setText("");
        statusBar.setText(" Analisando...");

        try {
            AnalisadorLexico lexico = new AnalisadorLexico(caminho);
            TabelaSimbolos tabela = new TabelaSimbolos();
            AnalisadorSintatico parser = new AnalisadorSintatico(lexico, tabela);

            parser.analisaPrograma();
            lexico.fechar();

            areaErro.setForeground(new Color(0, 150, 0));
            areaErro.setText("✓ Análise concluída sem erros.");
            statusBar.setText(" Compilação OK.");
        } catch (RuntimeException e) {
            areaErro.setForeground(Color.RED);
            areaErro.setText(e.getMessage());
            statusBar.setText(" Erro na análise.");
        } catch (IOException e) {
            areaErro.setText("Erro de leitura.");
            statusBar.setText(" Erro de IO.");
        }
    }

    private void irParaLinhaDoErro() {
        String msg = areaErro.getText();
        if (!msg.contains("linha")) return;

        try {
            int linha = Integer.parseInt(msg.split("linha")[1].trim().split(":")[0].trim());
            editor.destacarLinha(linha);
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
