package br.unesp.zoocard.backend.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.Graphics2D;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import br.unesp.zoocard.backend.exception.RegraNegocioException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImagemServiceTest {

    private final ImagemService service = new ImagemService();

    private byte[] pngDe(int largura, int altura) throws Exception {
        BufferedImage img = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    @Test
    void deveReduzirImagemGrandeParaNoMaximo512px() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "foto", "foto.png", "image/png", pngDe(2000, 1000));

        byte[] processada = service.processar(file);

        BufferedImage saida = ImageIO.read(new java.io.ByteArrayInputStream(processada));
        assertEquals(512, saida.getWidth());
        assertEquals(256, saida.getHeight(), "deve preservar a proporcao");
    }

    @Test
    void deveSubstituirTransparenciaPorFundoBrancoAoReduzir() throws Exception {
        int largura = 1000;
        int altura = 500;
        BufferedImage img = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        // Fundo totalmente transparente (alpha = 0).
        g.setComposite(java.awt.AlphaComposite.Clear);
        g.fillRect(0, 0, largura, altura);
        // Regiao opaca no centro, para garantir que o conteudo real nao foi afetado.
        g.setComposite(java.awt.AlphaComposite.Src);
        g.setColor(Color.BLUE);
        g.fillRect(largura / 2 - 50, altura / 2 - 50, 100, 100);
        g.dispose();

        ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
        ImageIO.write(img, "png", pngOut);
        MockMultipartFile file = new MockMultipartFile(
            "foto", "foto.png", "image/png", pngOut.toByteArray());

        byte[] processada = service.processar(file);

        BufferedImage saida = ImageIO.read(new java.io.ByteArrayInputStream(processada));
        // Canto que era transparente na origem deve virar branco (nao preto) na saida.
        int pixel = saida.getRGB(2, 2);
        int r = (pixel >> 16) & 0xFF;
        int gComp = (pixel >> 8) & 0xFF;
        int b = pixel & 0xFF;
        assertTrue(r > 250 && gComp > 250 && b > 250,
            "pixel que era transparente deveria virar branco, mas foi rgb(" + r + "," + gComp + "," + b + ")");
    }

    @Test
    void deveRejeitarArquivoQueNaoEImagem() {
        MockMultipartFile file = new MockMultipartFile(
            "foto", "doc.txt", "text/plain", "nao sou imagem".getBytes());

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> service.processar(file));
        assertEquals("O arquivo enviado não é uma imagem válida.", ex.getMessage());
    }

    @Test
    void dataUrlDeveTerPrefixoQueOImgTagAceita() {
        String url = service.toDataUrl(new byte[]{1, 2, 3}, "image/jpeg");
        assertTrue(url.startsWith("data:image/jpeg;base64,"), "Card.jsx usa isso direto em <img src>");
    }
}
