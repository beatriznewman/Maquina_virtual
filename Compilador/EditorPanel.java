import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;

public class EditorPanel extends JPanel {

    public final JTextArea txt;
    private final JTextArea linhas;
    private final JScrollPane scroll;

    public EditorPanel() {
        setLayout(new BorderLayout());

        // Editor principal
        txt = new JTextArea();
        txt.setFont(new Font("Consolas", Font.PLAIN, 14));
        txt.setTabSize(4);
        txt.setMargin(new Insets(6, 8, 6, 8));
        txt.setLineWrap(false);

        // Painel de números da esquerda (apenas visual)
        linhas = new JTextArea("1");
        linhas.setEditable(false);
        linhas.setFont(txt.getFont());
        linhas.setBackground(new Color(245, 245, 245));
        linhas.setForeground(new Color(120, 120, 120));
        linhas.setMargin(new Insets(6, 6, 6, 6));
        linhas.setFocusable(false);

        // Container com editor e números (números ficam como RowHeader)
        scroll = new JScrollPane(txt);
        scroll.setRowHeaderView(linhas);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200,200,200)));

        // Atualiza numeração quando o texto muda
        txt.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { atualizarLinhas(); }
            public void removeUpdate(DocumentEvent e)  { atualizarLinhas(); }
            public void insertUpdate(DocumentEvent e)  { atualizarLinhas(); }
        });

        // Sincroniza scroll vertical da coluna de números com o editor
        scroll.getVerticalScrollBar().addAdjustmentListener(e -> {
            // garante que a coluna de números acompanhe a rolagem
            JViewport view = scroll.getRowHeader();
            if (view != null) view.setViewPosition(new Point(0, scroll.getViewport().getViewPosition().y));
        });

        add(scroll, BorderLayout.CENTER);
        atualizarLinhas();
    }

    private void atualizarLinhas() {
        int linhasCount = Math.max(1, txt.getLineCount());
        int width = String.valueOf(linhasCount).length(); // número de dígitos
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= linhasCount; i++) {
            // right-aligned padding com espaços para ficar alinhado
            sb.append(String.format("%" + width + "d", i)).append("\n");
        }
        linhas.setText(sb.toString());
        // mantém mesma largura prévia para evitar "pulo" ao aumentar linhas
        int charWidth = txt.getFontMetrics(txt.getFont()).charWidth('0');
        int px = (width + 2) * charWidth;
        linhas.setPreferredSize(new Dimension(px, Integer.MAX_VALUE));
    }

    // Destaca uma linha (linha começa em 1)
    public void destacarLinha(int linha) {
        try {
            Highlighter h = txt.getHighlighter();
            h.removeAllHighlights();

            int start = txt.getLineStartOffset(linha - 1);
            int end   = txt.getLineEndOffset(linha - 1);

            h.addHighlight(start, end,
                new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 255, 160)));

            // move caret e rola para visualização
            txt.setCaretPosition(start);
            Rectangle r = txt.modelToView(start);
            if (r != null) {
                r.height = scroll.getViewport().getHeight(); // centraliza
                txt.scrollRectToVisible(r);
            }
        } catch (Exception ignored) {}
    }
}
