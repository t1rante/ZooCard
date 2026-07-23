package br.unesp.zoocard.backend.service;

import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.unesp.zoocard.backend.exception.RegraNegocioException;
import br.unesp.zoocard.backend.model.Animal;
import br.unesp.zoocard.backend.model.Carta;
import br.unesp.zoocard.backend.model.Colecao;
import br.unesp.zoocard.backend.model.Usuario;
import br.unesp.zoocard.backend.repository.AnimalRepository;
import br.unesp.zoocard.backend.repository.CartaRepository;
import br.unesp.zoocard.backend.repository.ColecaoRepository;

@Service
public class CartaGeneratorService {

    /** O verso da carta (Card.jsx) tem scroll proprio, entao comporta ate ~550 caracteres. */
    public static final int LIMITE_RESUMO = 550;

    /**
     * Confianca minima do BioCLIP (softmax sobre as especies candidatas) para aceitar
     * a identificacao. Abaixo disso a carta nao e gerada — evita vincular a foto a um
     * nome cientifico errado.
     */
    public static final double CONFIANCA_MINIMA = 0.30;

    private final ImagemService imagemService;
    private final BioClipClient bioClipClient;
    private final SpeciesDataService speciesDataService;
    private final AnimalRepository animalRepository;
    private final CartaRepository cartaRepository;
    private final ColecaoRepository colecaoRepository;

    public CartaGeneratorService(
        ImagemService imagemService,
        BioClipClient bioClipClient,
        SpeciesDataService speciesDataService,
        AnimalRepository animalRepository,
        CartaRepository cartaRepository,
        ColecaoRepository colecaoRepository
    ) {
        this.imagemService = imagemService;
        this.bioClipClient = bioClipClient;
        this.speciesDataService = speciesDataService;
        this.animalRepository = animalRepository;
        this.cartaRepository = cartaRepository;
        this.colecaoRepository = colecaoRepository;
    }

    public static String resumoOuFallback(String resumo, String nomeComum) {
        if (resumo == null || resumo.isBlank()) {
            return "Ainda não temos uma descrição para " + nomeComum
                + ". Esta carta guarda o registro do seu encontro com este animal.";
        }
        String limpo = resumo.trim();
        if (limpo.length() <= LIMITE_RESUMO) {
            return limpo;
        }
        return limpo.substring(0, LIMITE_RESUMO - 1).trim() + "…";
    }

    public static boolean confiancaSuficiente(double confidence) {
        return confidence >= CONFIANCA_MINIMA;
    }

    @Transactional
    public Carta gerar(MultipartFile foto, Usuario usuario) {
        byte[] jpeg = imagemService.processar(foto);

        var identificacao = bioClipClient.identificar(jpeg);
        if (!confiancaSuficiente(identificacao.confidence())) {
            throw new RegraNegocioException(
                "Não foi possível identificar o animal com confiança suficiente. Tente outra foto.");
        }
        var dados = speciesDataService.buscarComCache(identificacao.scientificName());

        float taxa = RarityCalculator.taxaExtincao(dados.codigoIucn(), dados.nomeCientifico());
        String nomeComum = dados.nomeComum();
        String resumo = resumoOuFallback(dados.resumo(), nomeComum);

        // reaproveita o Animal se ja existir: varias cartas apontam para o mesmo animal
        Animal animal = animalRepository.findByNomeIgnoreCase(nomeComum)
            .orElseGet(() -> {
                Animal novo = new Animal();
                novo.setNome(nomeComum);
                novo.setResumo(resumo);
                novo.setTaxaExtincao(taxa);
                novo.setWikipediaUrl(dados.wikipediaUrl());
                return animalRepository.save(novo);
            });

        Carta carta = new Carta();
        carta.setAnimal(animal);
        carta.setImagem(jpeg);
        carta.setImagemMimeType(ImagemService.MIME_SAIDA);
        carta.setCriadaEm(System.currentTimeMillis());
        carta.setColecoes(new ArrayList<>());
        Carta salva = cartaRepository.save(carta);

        adicionarNaColecaoGeral(salva, usuario);
        return salva;
    }

    private void adicionarNaColecaoGeral(Carta carta, Usuario usuario) {
        Colecao geral = colecaoRepository.findByUsuarioIdAndGeralTrue(usuario.getId())
            .orElseThrow(() -> new RegraNegocioException("Coleção Geral não encontrada para este usuário."));
        if (geral.getCartas() == null) {
            geral.setCartas(new ArrayList<>());
        }
        geral.getCartas().add(carta);
        colecaoRepository.save(geral);
    }
}
