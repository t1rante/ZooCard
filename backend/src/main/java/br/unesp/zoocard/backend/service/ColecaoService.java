package br.unesp.zoocard.backend.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import br.unesp.zoocard.backend.exception.RegraNegocioException;
import br.unesp.zoocard.backend.model.Colecao;
import br.unesp.zoocard.backend.repository.ColecaoRepository;
import br.unesp.zoocard.backend.repository.UsuarioRepository;

@Service
public class ColecaoService {

    private static final Pattern SUFIXO_COPIA = Pattern.compile("\\s+Cópia\\s+\\d+$");

    private final ColecaoRepository colecaoRepository;
    private final UsuarioRepository usuarioRepository;

    public ColecaoService(ColecaoRepository colecaoRepository, UsuarioRepository usuarioRepository) {
        this.colecaoRepository = colecaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Espelha a regra de nomeacao de copia do frontend (services/api.js). */
    public static String proximoNomeCopia(String nomeOrigem, List<String> nomesExistentes) {
        Matcher matcher = SUFIXO_COPIA.matcher(nomeOrigem);
        String base = matcher.find() ? nomeOrigem.substring(0, matcher.start()) : nomeOrigem;

        Pattern irma = Pattern.compile("^" + Pattern.quote(base) + " Cópia \\d+$");
        long copias = nomesExistentes.stream().filter(n -> irma.matcher(n).matches()).count();

        return base + " Cópia " + (copias + 1);
    }

    public List<Colecao> listar(Long usuarioId) {
        List<Colecao> colecoes = new ArrayList<>(colecaoRepository.findByUsuarioId(usuarioId));
        // geral sempre primeiro, como o frontend espera
        colecoes.sort(Comparator.comparing(Colecao::isGeral).reversed());
        return colecoes;
    }

    public Colecao criar(Long usuarioId, String nome) {
        var usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado."));

        Colecao colecao = new Colecao();
        colecao.setNome(nome);
        colecao.setGeral(false);
        colecao.setUsuario(usuario);
        colecao.setCartas(new ArrayList<>());
        return colecaoRepository.save(colecao);
    }

    public Colecao duplicar(Long colecaoId) {
        Colecao origem = buscar(colecaoId);
        if (origem.isGeral()) {
            throw new RegraNegocioException("A Coleção Geral não pode ser duplicada.");
        }

        List<String> nomes = colecaoRepository.findByUsuarioId(origem.getUsuario().getId())
            .stream().map(Colecao::getNome).toList();

        Colecao copia = new Colecao();
        copia.setNome(proximoNomeCopia(origem.getNome(), nomes));
        copia.setGeral(false);
        copia.setUsuario(origem.getUsuario());
        copia.setCartas(new ArrayList<>(origem.getCartas()));
        return colecaoRepository.save(copia);
    }

    public void deletar(Long colecaoId) {
        Colecao colecao = buscar(colecaoId);
        if (colecao.isGeral()) {
            throw new RegraNegocioException("A Coleção Geral não pode ser deletada.");
        }
        colecaoRepository.delete(colecao);
    }

    public void removerCarta(Long cartaId, Long colecaoId) {
        Colecao colecao = buscar(colecaoId);
        if (colecao.isGeral()) {
            throw new RegraNegocioException("Não é possível remover cartas da Coleção Geral.");
        }
        colecao.getCartas().removeIf(c -> c.getId().equals(cartaId));
        colecaoRepository.save(colecao);
    }

    private Colecao buscar(Long colecaoId) {
        return colecaoRepository.findById(colecaoId)
            .orElseThrow(() -> new RegraNegocioException("Coleção não encontrada."));
    }
}
