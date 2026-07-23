package br.unesp.zoocard.backend.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import br.unesp.zoocard.backend.exception.RegraNegocioException;

@Service
public class ImagemService {

    /** Lado maior maximo, em pixels. Mantem a data URL em ~40-60 KB. */
    public static final int LADO_MAXIMO = 512;
    public static final String MIME_SAIDA = "image/jpeg";
    private static final long TAMANHO_MAXIMO_BYTES = 10L * 1024 * 1024;

    public byte[] processar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RegraNegocioException("Nenhuma imagem foi enviada.");
        }
        if (file.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new RegraNegocioException("A imagem excede o tamanho máximo de 10 MB.");
        }

        BufferedImage original;
        try {
            original = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
        } catch (IOException e) {
            throw new RegraNegocioException("O arquivo enviado não é uma imagem válida.");
        }
        // ImageIO.read devolve null quando o conteudo nao e uma imagem reconhecida
        if (original == null) {
            throw new RegraNegocioException("O arquivo enviado não é uma imagem válida.");
        }

        BufferedImage reduzida = reduzir(original);
        return paraJpeg(reduzida);
    }

    public String toDataUrl(byte[] bytes, String mimeType) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    private BufferedImage reduzir(BufferedImage original) {
        int largura = original.getWidth();
        int altura = original.getHeight();
        int ladoMaior = Math.max(largura, altura);

        if (ladoMaior <= LADO_MAXIMO) {
            return converterParaRgb(original);
        }

        double escala = (double) LADO_MAXIMO / ladoMaior;
        int novaLargura = Math.max(1, (int) Math.round(largura * escala));
        int novaAltura = Math.max(1, (int) Math.round(altura * escala));

        BufferedImage destino = new BufferedImage(novaLargura, novaAltura, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, novaLargura, novaAltura, java.awt.Color.WHITE, null);
        g.dispose();
        return destino;
    }

    /** JPEG nao suporta canal alfa; PNG transparente viraria imagem preta sem esta conversao. */
    private BufferedImage converterParaRgb(BufferedImage origem) {
        if (origem.getType() == BufferedImage.TYPE_INT_RGB) {
            return origem;
        }
        BufferedImage destino = new BufferedImage(
            origem.getWidth(), origem.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        g.drawImage(origem, 0, 0, java.awt.Color.WHITE, null);
        g.dispose();
        return destino;
    }

    private byte[] paraJpeg(BufferedImage imagem) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(imagem, "jpg", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RegraNegocioException("Falha ao processar a imagem.");
        }
    }
}
