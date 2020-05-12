package com.github.nidorx.http;

import com.github.nidorx.http.util.BiConsumerThrowable;
import com.github.nidorx.http.util.Callback;
import com.github.nidorx.http.gson.Gson;

import javax.activation.UnsupportedDataTypeException;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * Objeto padrão para consultas http
 *
 * @author Alex Rodin <contato@alexrodin.info>
 */
public final class HttpRequest {

    public static final String HEADER_HOST = "Host";

    public static final String HEADER_COOKIE = "Cookie";

    public static final String HEADER_ACCEPT = "Accept";

    public static final String HEADER_ORIGIN = "Origin";

    public static final String HEADER_USER_AGENT = "User-Agent";

    public static final String HEADER_SET_COOKIE = "Set-Cookie";

    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    public static final String HEADER_AUTHORIZATION = "Authorization";

    public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";

    public static final String HEADER_CONTENT_LANGUAGE = "Content-Language";

    public static final String APPLICATION_JSON = "application/json; charset=utf-8";

    public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded; charset=UTF-8";

    private static final Gson OBJECT_MAPPER = new Gson();
    /**
     * Salva a compilação dos regex usados para fazer alteração no PATH de endpoints
     */
    private static final Map<String, Pattern> PATH_REGEX_CACHED = new ConcurrentHashMap<>();
    /**
     * Permite a depuração dos detalhes da requisição sendo efetuada
     */
    public static boolean DEBUG = false;

    private final String url;

    private final Map<String, String> path = new HashMap<>();

    private final Map<String, String> headers = new HashMap<>();

    private final Map<String, List<String>> query = new HashMap<>();

    private Object data;

    private int timeout;

    private boolean binary;

    private String method;

    private String userAgent;

    private String contentType;

    private CookieManager cookieManager;

    private BiConsumerThrowable<HttpResponse, Map<String, Object>> onError;

    private BiConsumerThrowable<HttpResponse, Map<String, Object>> onSuccess;

    private BiConsumerThrowable<HttpResponse, Map<String, Object>> onComplete;

    /**
     * Permite a criação de uma configuração fora deste contexto, para ser reaproveitada em requisições diversas
     *
     * @param baseUrl
     */
    public HttpRequest(final String baseUrl) {
        this.url = baseUrl;
        this.method = "GET";
        // Timeout default de 30 segundos
        this.timeout = 30000;
        this.contentType = APPLICATION_X_WWW_FORM_URLENCODED;
        this.userAgent = UserAgentList.getRandom();
        this.cookieManager = new CookieManager();
    }

    public static HttpRequest build(String baseUrl) {
        return new HttpRequest(baseUrl);
    }

    /**
     * Gera a query string para compor a url final
     *
     * @param queryParams
     * @return
     */
    public static String getQueryString(final Map<String, List<String>> queryParams) throws UnsupportedEncodingException {
        StringBuilder queryString = new StringBuilder();
        String charset = StandardCharsets.UTF_8.toString();
        if (queryParams != null) {
            boolean first = true;
            for (Map.Entry<String, List<String>> param : queryParams.entrySet()) {
                final String key = param.getKey();
                for (String vl : param.getValue()) {
                    if (first) {
                        first = false;
                    } else {
                        queryString.append('&');
                    }
                    queryString.append(URLEncoder.encode(key, charset));
                    queryString.append('=');
                    queryString.append(URLEncoder.encode(vl, charset));
                }
            }
        }
        String out = queryString.toString();
        return out.isEmpty() ? "" : "?" + out;
    }

    /**
     * Gera os bytes quando o tipo de requisição for um POST ou PUT
     *
     * @param postData
     * @return
     */
    private static byte[] generatePostData(Object postData, final String contentType) throws IOException {

        if (postData == null) {
            return null;
        }

        if (APPLICATION_JSON.equals(contentType)) {
            return OBJECT_MAPPER.toJson(postData).getBytes(StandardCharsets.UTF_8);
        } else if (APPLICATION_X_WWW_FORM_URLENCODED.equals(contentType)) {
            if (!(postData instanceof Map)) {
                throw new UnsupportedDataTypeException("Post data need to be Map<String, String>");
            }

            Map<String, String> data = (Map<String, String>) postData;
            StringBuilder dataString = new StringBuilder();
            String charset = StandardCharsets.UTF_8.toString();
            if (data != null) {
                boolean first = true;
                for (Map.Entry<String, String> param : data.entrySet()) {
                    if (first) {
                        first = false;
                    } else {
                        dataString.append('&');
                    }
                    dataString.append(URLEncoder.encode(param.getKey(), charset));
                    dataString.append('=');
                    dataString.append(URLEncoder.encode(param.getValue(), charset));
                }
            }

            return dataString.toString().getBytes(StandardCharsets.UTF_8);
        }

        throw new UnsupportedDataTypeException("Invalid content-type");
    }

    private static void debugPostData(final Object postData, final String contentType) {
        if (APPLICATION_JSON.equals(contentType)) {
            System.out.println("Form Data (JSON)");
            System.out.println("    " + OBJECT_MAPPER.toJson(postData));
            OBJECT_MAPPER.toJson(postData);
            System.out.println("------------------------------------------------------");
        } else if (postData instanceof Map) {
            Map<String, String> data = (Map<String, String>) postData;
            System.out.println("Form Data");
            for (Map.Entry<String, String> param : data.entrySet()) {
                System.out.println("    " + param.getKey() + ": " + param.getValue());
            }
            System.out.println("------------------------------------------------------");
        }
    }

    /**
     * The HTTP method to use for the request (e.g. "POST", "GET", "PUT").
     *
     * @param method
     * @return
     */
    public HttpRequest method(final String method) {
        this.method = method.toUpperCase();
        return this;
    }

    /**
     * Define o timeout da requisição. Padrao de 30 segundos
     *
     * @param timeout
     * @return
     */
    public HttpRequest timeout(final int timeout) {
        this.timeout = timeout;
        return this;
    }


    /**
     * Define que o resultado esperado é um binário. Ex. Download
     *
     * @param binary
     * @return
     */
    public HttpRequest binary(final boolean binary) {
        this.binary = binary;
        return this;
    }

    /**
     * Define o UserAgent
     *
     * @param userAgent
     * @return
     */
    public HttpRequest userAgent(final String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    /**
     * When sending data to the server, use this content type.
     * <p>
     * Default is "application/x-www-form-urlencoded; charset=UTF-8", which is fine for most cases.
     *
     * @param contentType
     * @return
     */
    public HttpRequest contentType(final String contentType) {
        this.contentType = contentType;
        return this;
    }

    /**
     * Permite definir o gerenciador de cookies dessa requisição
     *
     * @param cookieManager
     * @return
     */
    public HttpRequest cookieManager(final CookieManager cookieManager) {
        this.cookieManager = cookieManager;
        return this;
    }

    /**
     * Permite definir parametros do path
     *
     * @param key
     * @param value
     * @return
     */
    public HttpRequest path(final String key, final String value) {
        if (key == null || key.isEmpty() || value == null || value.isEmpty()) {
            return this;
        }
        this.path.put(key, value);
        return this;
    }

    /**
     * Permite setar um cookie de requisição
     *
     * @param key
     * @param value
     * @return
     */
    public HttpRequest cookie(final String key, final String value) {
        if (key == null || key.isEmpty() || value == null || value.isEmpty()) {
            return this;
        }
        this.cookieManager.getCookieStore().add(null, new HttpCookie(key, value));
        return this;
    }

    /**
     * Appended to the url requests
     * <p>
     * It is converted to a query string.
     * <p>
     * Object must be Key/Value pairs.
     * <p>
     * If value is an Array, will serializes multiple values with same key based on the value of the traditional
     * setting
     *
     * @param key
     * @param value
     * @return
     */
    public HttpRequest query(final String key, final String value) {
        if (key == null || key.isEmpty() || value == null) {
            return this;
        }
        if (!this.query.containsKey(key)) {
            this.query.put(key, new ArrayList<>());
        }

        this.query.get(key).add(value);

        return this;
    }

    /**
     * Appended to the url requests
     * <p>
     * It is converted to a query string.
     * <p>
     * Object must be Key/Value pairs.
     * <p>
     * If value is an Array, will serializes multiple values with same key based on the value of the traditional
     * setting
     *
     * @param query
     * @return
     */
    public HttpRequest query(Map<String, List<String>> query) {
        if (query == null || query.isEmpty()) {
            return this;
        }

        for (Map.Entry<String, List<String>> e : query.entrySet()) {
            String key = e.getKey();
            for (String s : e.getValue()) {
                this.query(key, s);
            }
        }

        return this;
    }

    /**
     * Data to be sent to the server.
     * <p>
     * Object must be Key/Value pairs.
     *
     * @param data
     * @return
     */
    public HttpRequest data(final Object data) {
        if (data == null) {
            return this;
        }

        this.data = data;

        return this;
    }

    /**
     * Data to be sent to the server.
     * <p>
     * Object must be Key/Value pairs.
     *
     * @param key
     * @param value
     * @return
     */
    public HttpRequest data(final String key, final String value) {
        if (key == null || key.isEmpty()) {
            return this;
        }

        if (!(this.data instanceof Map)) {
            this.data = new HashMap<String, String>();
        }
        ((Map<String, String>) this.data).put(key, value);
        return this;
    }

    /**
     * Data to be sent to the server.
     * <p>
     * Object must be Key/Value pairs.
     *
     * @param data Map[String, List[String]] OR Map[String, String]
     * @return
     */
    public HttpRequest data(final Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return this;
        }
        if (!(this.data instanceof Map)) {
            this.data = new HashMap<String, String>();
        }

        ((Map<String, String>) this.data).putAll(data);
        return this;
    }

    /**
     * An object of additional header key/value pairs to send along with requests using the HttpRequest transport.
     *
     * @param key
     * @param value
     * @return
     */
    public HttpRequest header(final String key, final String value) {
        if (key.equalsIgnoreCase(HEADER_COOKIE)) {
            // Se for cookie, adiciona usando o método específico
            final String[] parts = value.split(";");
            for (String cookie : parts) {
//                final String[] split = part.split("=");
//                if (split.length < 2) {
//                    continue;
//                }
                this.cookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
//                cookie(split[0], split[1]);
            }
        } else {
            this.headers.put(key, value);
        }
        return this;
    }

    /**
     * Adicionar os cabeçalhos de uma autenticação Basic
     *
     * @param username
     * @param password
     * @return
     */
    public HttpRequest authBasic(String username, String password) {
        String auth = username + ":" + password;
        String hash = new String(Base64.getEncoder().encode(auth.getBytes(Charset.forName("US-ASCII"))));
        this.header(HEADER_AUTHORIZATION, "Basic " + hash);
        return this;
    }

    /**
     * A function to be called if the request succeeds.
     *
     * @param succes
     * @return
     */
    public HttpRequest success(final BiConsumerThrowable<HttpResponse, Map<String, Object>> succes) {
        this.onSuccess = succes;
        return this;
    }

    /**
     * A function to be called if the request fails.
     *
     * @param error
     * @return
     */
    public HttpRequest error(final BiConsumerThrowable<HttpResponse, Map<String, Object>> error) {
        this.onError = error;
        return this;
    }

    /**
     * A function to be called when the request finishes (after success and error callbacks are executed).
     * <p>
     * The function gets passed two arguments: The HttpResponse object and a boolean categorizing the status of the
     * request [true: success, false: error]
     *
     * @param complete
     * @return
     */
    public HttpRequest complete(final BiConsumerThrowable<HttpResponse, Map<String, Object>> complete) {
        this.onComplete = complete;
        return this;
    }

    public HttpResponse execute() throws Exception {
        return execute((HttpResponse response, Map<String, Object> context) -> {
            return response;
        });
    }

    public <T> T execute(final Callback<HttpResponse, Map<String, Object>, T> callback) throws Exception {
        final HttpResponse response = executeRequest();
        final Map<String, Object> context = new HashMap<>();
        if (response.statusCode < 400 && onSuccess != null) {
            // On success
            onSuccess.accept(response, context);
        } else if (response.statusCode >= 400 && onError != null) {
            // On Error
            onError.accept(response, context);
        }

        // On Complete
        if (onComplete != null) {
            onComplete.accept(response, context);
        }

        // Callback
        return callback.apply(response, context);
    }

    /**
     * Gera a url final de uma requisição, adicionando os query params e path params necessários
     *
     * @return
     */
    public String getFinalUrl() throws UnsupportedEncodingException {
        String finalUrl = url;
        for (Map.Entry<String, String> param : path.entrySet()) {
            String key = param.getKey();
            if (!PATH_REGEX_CACHED.containsKey(key)) {
                PATH_REGEX_CACHED.put(key, Pattern.compile("\\{" + key + "\\}"));
            }
            finalUrl = PATH_REGEX_CACHED.get(key).matcher(finalUrl).replaceAll(param.getValue());
        }
        finalUrl += getQueryString(query);
        return finalUrl;
    }

    /**
     * Executa a requisição
     */
    private <T> HttpResponse executeRequest() throws IOException {
        final String finalUrl = getFinalUrl();

        Reader reader = null;
        BufferedReader in = null;
        InputStream inputStream = null;
        DataOutputStream out = null;
        try {
            final URL connUrl = new URL(finalUrl);
            final HttpURLConnection connection = (HttpURLConnection) connUrl.openConnection();

            if (method.equals("PATCH")) {
                // https://stackoverflow.com/a/32503192
                connection.setRequestProperty("X-HTTP-Method-Override", "PATCH");
                method = "POST";
            }

            connection.setRequestMethod(method);

            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);

            // Adiciona os headers básicos de uma requisição
            if (!headers.containsKey(HEADER_ACCEPT)) {
                connection.setRequestProperty(HEADER_ACCEPT, "*/*");
            }
            connection.setRequestProperty(HEADER_HOST, connUrl.getHost());
            connection.setRequestProperty(HEADER_ORIGIN, connUrl.getProtocol() + "://" + connUrl.getHost());

            System.setProperty("http.agent", "");

            connection.setRequestProperty(HEADER_USER_AGENT, this.userAgent);
            connection.setRequestProperty(HEADER_CONTENT_LANGUAGE, "en-US");
            connection.setRequestProperty(HEADER_ACCEPT_ENCODING, "gzip");


            if (method.equals("POST") || method.equals("PUT")) {
                connection.setRequestProperty(HEADER_CONTENT_TYPE, contentType);
            }

            // Setar os headers da conexão (Sobrescreve os já definidos)
            headers.entrySet().stream().forEach((header) -> {
                connection.setRequestProperty(header.getKey(), header.getValue());
            });

            // Setar os cookies da requisição
            if (this.cookieManager != null && this.cookieManager.getCookieStore().getCookies().size() > 0) {
                String cookies = this.cookieManager.getCookieStore().getCookies().stream()
                        .map(cookie -> cookie.toString())
                        .collect(Collectors.joining(";"));
                if (!cookies.isEmpty()) {
                    connection.setRequestProperty(HEADER_COOKIE, cookies);
                }
            }

            if (DEBUG) {
                System.out.println("\n======================================================");
                System.out.println("General");
                System.out.println("    Request URL: " + finalUrl);
                System.out.println("    Request Method: " + method);
                System.out.println("------------------------------------------------------");

                System.out.println("Request Headers");
                connection.getRequestProperties().keySet().stream().sorted().forEach(header -> {
                    System.out.println("    " + header + ": " + connection.getRequestProperty(header));
                });
            }

            // Timeout de requisição
            if (this.timeout >= 0) {
                connection.setConnectTimeout(this.timeout);
            }

            // Enviar dados, formulário
            if ((method.equals("POST") || method.equals("PUT")) && this.data != null) {
                connection.setDoOutput(true);
                final byte[] data = generatePostData(this.data, this.contentType);
                connection.setRequestProperty("Content-Length", Integer.toString(data.length));

                if (DEBUG) {
                    System.out.println("    Content-Length: " + data.length);
                    debugPostData(this.data, this.contentType);
                }

                out = new DataOutputStream(connection.getOutputStream());
                out.write(data);
                out.close();
            }

            final HttpResponse response = new HttpResponse();


            response.statusCode = connection.getResponseCode();

            if (response.statusCode < 400) {
                // Informational || OK || Redirect
                inputStream = connection.getInputStream();
            } else {
                // Error
                inputStream = connection.getErrorStream();
            }

            if ("gzip".equals(connection.getContentEncoding())) {
                inputStream = new GZIPInputStream(inputStream);
            }


            // Obtém os headers da conexão
            response.headers = new HashMap<>(connection.getHeaderFields());

            // Adiciona os novos cookies no gerenciador
            List<String> cookiesHeader = response.headers.get(HEADER_SET_COOKIE);
            if (this.cookieManager != null && cookiesHeader != null) {
                for (String cookie : cookiesHeader) {
                    this.cookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
                }
            }

            if (DEBUG) {
                System.out.println("Response Headers");
                response.headers.keySet().stream()
                        .filter(s -> {
                            return s != null && !s.isEmpty();
                        })
                        .sorted()
                        .forEach(header -> {
                            System.out.println("    " + header + ": " + response.headers.get(header).stream().collect(Collectors.joining("")));

                        });
                System.out.println("======================================================\n");
            }

            // Seta a referencia para o gerenciador de cookie usado na resposta
            response.cookieManager = this.cookieManager;

            if (this.binary) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                int nRead;
                byte[] data = new byte[16384];

                while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }

                response.data = buffer.toByteArray();
            } else {

                // Accept-Encoding : gzip
                reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

                in = new BufferedReader(reader);
                final StringBuilder body = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    body.append(inputLine);
                    body.append('\r');
                }
                response.content = body.toString();
            }


            return response;
        } finally {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (reader != null) {
                reader.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }
}

