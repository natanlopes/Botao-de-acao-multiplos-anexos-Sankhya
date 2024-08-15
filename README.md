# Botão ação Anexos campo multiplos arquivos- Sankhya
## Descrição
A classe BotaoAnexos é um plugin para a plataforma Sankhya que processa anexos e envia e-mails com esses anexos. Este plugin é integrado como uma ação personalizada e realiza as seguintes operações:

Geração de Arquivos: Recupera arquivos com base em informações extraídas de um campo JSON e salva esses arquivos.

Envio de E-mails: Cria e-mails com os arquivos anexados e os envia para destinatários especificados.

## Funcionalidade
doAction(ContextoAcao contexto)

Método principal chamado para processar o anexo e enviar o e-mail:

Obtém ID: Recupera o ID da linha selecionada no contexto da ação.

Chama gerarArquivo: Processa os arquivos associados ao ID e os salva.

Fecha a Sessão: Garante que a sessão JAPE seja fechada após o processamento.

gerarArquivo(ContextoAcao ctx, BigDecimal nuanexo)

Método que realiza a geração e salvamento dos arquivos:

Lê Dados do Arquivo: Obtém e lê os dados do arquivo a partir do contexto da ação.

Extrai Informações de Arquivo: Usa JSON para obter informações sobre os arquivos a partir do conteúdo do arquivo.

Salva Arquivos: Lê e salva os arquivos do repositório.

Chama enviarEmailComAnexos: Envia e-mails com os arquivos anexados.

extrairJson(String contentInfo)

Método que extrai e deserializa informações JSON de uma string:


Extrai JSON: Usa uma expressão regular para encontrar e extrair JSON do conteúdo fornecido.

Deserializa JSON: Converte o JSON em uma lista de objetos FileInformation.

enviarEmailComAnexos(EntityFacade dwfEntityFacade, ContextoAcao contexto, List<File> arquivosParaEnviar, List<FileInformation> fileInformations)
## Método que cria e envia e-mails com anexos:

- Cria Mensagem: Insere uma nova entrada na tabela MSDFilaMensagem para criar uma mensagem.
- Cria Anexos: Insere cada anexo na tabela AnexoMensagem e associa-os à mensagem.
- Associa Anexos à Mensagem: Adiciona os anexos à mensagem na tabela AnexoPorMensagem.
# Estrutura do Código
## Imports
O código utiliza várias bibliotecas e classes, incluindo:

```br.com.sankhya.extensions.actionbutton.AcaoRotinaJava
br.com.sankhya.extensions.actionbutton.ContextoAcao
br.com.sankhya.extensions.actionbutton.Registro
br.com.sankhya.jape.EntityFacade
br.com.sankhya.jape.bmp.PersistentLocalEntity
br.com.sankhya.jape.core.JapeSession
br.com.sankhya.jape.dao.JdbcWrapper
br.com.sankhya.jape.sql.NativeSql
br.com.sankhya.jape.vo.DynamicVO
br.com.sankhya.jape.vo.EntityVO
br.com.sankhya.jape.wrapper.JapeFactory
br.com.sankhya.modelcore.util.EntityFacadeFactory
br.com.sankhya.modelcore.util.SWRepositoryUtils
com.fasterxml.jackson.core.type.TypeReference
com.fasterxml.jackson.databind.ObjectMapper
com.sankhya.util.BigDecimalUtil
com.sankhya.util.TimeUtils
org.apache.commons.io.FileUtils
 ```
## Erros e Mensagens
 - Tratamento de Erros: Em caso de erro durante o processamento ou envio, a mensagem de erro é configurada e exibida.
- Mensagens de Retorno: Mensagens de sucesso ou erro são definidas no contexto da ação.
