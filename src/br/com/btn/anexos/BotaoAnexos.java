package br.com.btn.anexos;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.SWRepositoryUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sankhya.util.BigDecimalUtil;
import com.sankhya.util.TimeUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BotaoAnexos implements AcaoRotinaJava {
    //    BigDecimal nuAnexo;
    String msg;
    @Override

    public void doAction(ContextoAcao contexto) throws Exception {
        JapeSession.SessionHandle hnd = null;
        Registro linha = contexto.getLinhas()[0];
        EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
        BigDecimal ID = (BigDecimal) linha.getCampo("ID");//PEGANDO O ID DA TELA DE ANEXOS PODE SER A PK

        try {
            gerarArquivo(contexto, ID);

        } finally {
            JapeSession.close(hnd);
        }
    }
    public void gerarArquivo(ContextoAcao ctx, BigDecimal nuanexo) throws Exception {
        JdbcWrapper jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
        JapeSession.SessionHandle hnd = JapeSession.open();
        EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
        List<File> arquivosSalvos = new ArrayList<>();

        try {

            byte[] dados = (byte[]) ctx.getLinhas()[0].getCampo("ARQUIVO");
            String contentInfo = new String(dados, StandardCharsets.ISO_8859_1);
            List<FileInformation> fileInformations = extrairJson(contentInfo);

            for (FileInformation fileInfo : fileInformations) {
                File file = SWRepositoryUtils.getFile(fileInfo.getPath() + "/" + fileInfo.getInternalName());
                byte[] fileContent = FileUtils.readFileToByteArray(file); // Lê o conteúdo completo do arquivo
                FileUtils.writeByteArrayToFile(file, fileContent); // Essa linha pode ser desnecessária se você só precisa enviar o arquivo
                arquivosSalvos.add(file);
            }

            ctx.setMensagemRetorno("Arquivo(s) recuperado(s) com sucesso, verifique o repositório de arquivo.");
            enviarEmailComAnexos(dwfFacade, ctx, arquivosSalvos,fileInformations);
        } catch (Exception ex) {
            ctx.mostraErro(ex.getMessage());
        } finally {
            JapeSession.close(hnd);
        }
    }
    private List<FileInformation> extrairJson(String contentInfo) throws IOException {
        String patternString = "__start_fileinformation__(.*?)__end_fileinformation__";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(contentInfo);

        if (matcher.find()) {
            String extractedJson = matcher.group(1);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(extractedJson, new TypeReference<List<FileInformation>>() {});
        } else {
            throw new IOException("JSON não encontrado entre os marcadores especificados.");
        }
    }
    public void enviarEmailComAnexos(EntityFacade dwfEntityFacade, ContextoAcao contexto, List<File> arquivosParaEnviar,List<FileInformation> fileInformations)	throws Exception {
        BigDecimal codFila = null;
        BigDecimal seq = null;
        BigDecimal nuAnexo = null;
        JdbcWrapper jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
        NativeSql nativeSql = new NativeSql(jdbc);

        try {
            // Passo 1: Criação da Mensagem na MSDFilaMensagem para obter o CODFILA
            DynamicVO dynamicVO1 = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("MSDFilaMensagem");
            dynamicVO1.setProperty("ASSUNTO", "Documentos Importantes");
            dynamicVO1.setProperty("CODMSG", null);
            dynamicVO1.setProperty("DTENTRADA", TimeUtils.getNow());
            dynamicVO1.setProperty("STATUS", "Pendente");
            dynamicVO1.setProperty("CODCON", BigDecimal.ZERO);
            dynamicVO1.setProperty("TENTENVIO", BigDecimalUtil.valueOf(3));
            dynamicVO1.setProperty("MENSAGEM", new String("Mensagem de teste").toCharArray());
            dynamicVO1.setProperty("TIPOENVIO", "E");
            dynamicVO1.setProperty("MAXTENTENVIO", BigDecimalUtil.valueOf(3));
            dynamicVO1.setProperty("EMAIL", "SEU EMAIL");
            dynamicVO1.setProperty("CODSMTP", null);
            dynamicVO1.setProperty("CODUSUREMET", contexto.getUsuarioLogado());
            dynamicVO1.setProperty("MIMETYPE", "text/html");
            PersistentLocalEntity createEntity = dwfEntityFacade.createEntity("MSDFilaMensagem", (EntityVO) dynamicVO1);
            codFila = ((DynamicVO) createEntity.getValueObject()).asBigDecimal("CODFILA");

            if (arquivosParaEnviar.size() != fileInformations.size()) {
                throw new IllegalArgumentException("Listas de arquivos e informações não correspondem em tamanho.");
            }

            for(int i = 0; i < arquivosParaEnviar.size(); i++) {
                File arquivo = arquivosParaEnviar.get(i);
                FileInformation fileInfo = fileInformations.get(i);
                byte[] fileContent = FileUtils.readFileToByteArray(arquivo);
                // Passo 2: Criação dos Anexos na AnexoMensagem para obter os NUANEXO
                DynamicVO dynamicVO2 = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("AnexoMensagem");
                dynamicVO2.setProperty("ANEXO",fileContent);
                dynamicVO2.setProperty("NOMEARQUIVO",arquivo.getName());
                dynamicVO2.setProperty("TIPO",fileInfo.getType());
                createEntity = dwfEntityFacade.createEntity("AnexoMensagem", (EntityVO) dynamicVO2);
                nuAnexo = ((DynamicVO) createEntity.getValueObject()).asBigDecimal("NUANEXO");
                // Passo 3: Associação dos Anexos à Mensagem na TMDAXM
                DynamicVO dynamicVO3 = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("AnexoPorMensagem");
                dynamicVO3.setProperty("CODFILA", codFila);
                dynamicVO3.setProperty("NUANEXO", nuAnexo);
                dwfEntityFacade.createEntity("AnexoPorMensagem", (EntityVO) dynamicVO3);
            }

        } catch (Exception e) {
            e.printStackTrace();
            msg = "Erro na criação da mensagem: " + e.getMessage();
            contexto.setMensagemRetorno(msg);
        }
    }
    public static class FileInformation {
        private String name; //Nome do arquivo, como ele é reconhecido pelo usuário.
        private int size;//Tamanho do arquivo em bytes.
        private String type;//Tipo MIME do arquivo, que indica o formato do arquivo.
        private String internalName;// Nome interno do arquivo, possivelmente um identificador único ou um nome de arquivo em um sistema de armazenamento.
        private String path;//Caminho do arquivo no sistema de armazenamento
        private String lastModifiedDate;//

        // Getters e setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getInternalName() { return internalName; }
        public void setInternalName(String internalName) { this.internalName = internalName; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getLastModifiedDate() { return lastModifiedDate; }
        public void setLastModifiedDate(String lastModifiedDate) { this.lastModifiedDate = lastModifiedDate; }
    }
}
