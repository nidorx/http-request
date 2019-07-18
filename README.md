[![](https://jitpack.io/v/nidorx/http-request.svg)](https://jitpack.io/#nidorx/http-request)

# http-request

A very small (160KB, zero dependency) HTTP client for Java using HttpURLConnection


## Install with Maven, on pom.xml:

```
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.nidorx</groupId>
        <artifactId>http-request</artifactId>
        <version>1.0.2</version>
    </dependency>
</dependencies>
```


## Object JSON request

### simple

```java
import com.github.nidorx.http.HttpRequest;

import java.util.Map;

public class HttpRequestSimpleDemo {

    public static void main(String[] args) throws Exception {

        String githubUser = "nidorx";
        String githubRepoName = "http-request";

        // https://api.github.com/repos/nidorx/http-request
        Map<String, Object> repo = HttpRequest.build("https://api.github.com/repos/{USER}/{REPO}")
                .path("USER", githubUser)
                .path("REPO", githubRepoName)
                .execute((response, context) -> {

                    return response.fromJson();
                });


        System.out.println(repo.get("description"));
    }
}
```

### Typed resource

```java
import com.github.nidorx.http.HttpRequest;

public class HttpRequestObjectTyped {

    public static void main(String[] args) throws Exception {

        String githubUser = "nidorx";
        String githubRepoName = "http-request";

        // https://api.github.com/repos/nidorx/http-request
        Repository repo = HttpRequest.build("https://api.github.com/repos/{USER}/{REPO}")
                .path("USER", githubUser)
                .path("REPO", githubRepoName)
                .execute((response, context) -> {

                    return response.fromJson(Repository.class);
                });

        System.out.println(repo.getDescription());
    }


    private static final class Repository {

        String description;

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
```


## Array JSON request

### Simple

```java
import com.github.nidorx.http.HttpRequest;

import java.util.List;
import java.util.Map;

public class HttpRequestArraySimple {

    public static void main(String[] args) throws Exception {

        String githubUser = "nidorx";

        // https://api.github.com/users/nidorx/repos?per_page=5
        List<Map<String, Object>> repos = HttpRequest.build("https://api.github.com/users/{USER}/repos")
                .path("USER", githubUser)
                .query("per_page", "5")
                .execute((response, context) -> {

                    return response.fromJsonToList();
                });

        repos.forEach(repo -> {
            System.out.println(repo.get("description"));
        });
    }
}
```

### Typed resource

```java
import com.github.nidorx.http.HttpRequest;
import com.github.nidorx.http.util.ParameterizedTypeReference;

import java.util.List;

public class HttpRequestDemo {

    public static void main(String[] args) throws Exception {

        String githubUser = "nidorx";

        // https://api.github.com/users/nidorx/repos?per_page=5
        List<Repository> content = HttpRequest.build("https://api.github.com/users/{USER}/repos")
                .path("USER", githubUser)
                .query("per_page", "5")
                .execute((response, context) -> {

                    return response.fromJson(new ParameterizedTypeReference<List<Repository>>() {});
                });

        content.forEach(repository -> {
            System.out.println(repository.getDescription());
        });
    }

    private static final class Repository {

        String description;

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
```


## String response (xml, csv, html, etc)

Use `response.content`

```java
import com.github.nidorx.http.HttpRequest;

public class HttpRequestString {

    public static void main(String[] args) throws Exception {

        String htmlContent = HttpRequest.build("https://www.google.com")
                .execute((response, stringObjectMap) -> {

                    return response.content;
                });

        System.out.println(htmlContent);
    }
}
```

## Binary download

Set ` .binary(true)` to get `response.data`;

```java
import com.github.nidorx.http.HttpRequest;

import java.io.File;
import java.io.FileOutputStream;

public class HttpRequestBinary {

    public static void main(String[] args) throws Exception {

        HttpRequest.build("https://en.wikipedia.org/static/images/project-logos/enwiki.png")
                .binary(true)
                .method("GET")
                .execute((response, stringObjectMapd) -> {

                    byte[] data = response.data;
                    FileOutputStream fos = new FileOutputStream(new File("WIKI_LOGO.png"));
                    fos.write(data, 0, data.length);
                    fos.flush();
                    fos.close();

                    return null;
                });
    }
}
```


## Using flow (Context, onSucces, onError, onComplete)

```java
import com.github.nidorx.http.HttpRequest;

public class HttpRequestFlow {

    public static void main(String[] args) throws Exception {

        HttpRequest.build("https://www.google.com")
                .success((response, context) -> {
                    context.put("value on success", true);

                    System.out.println("Success!");
                })
                .error((response, context) -> {
                    context.put("value on success", true);

                    System.out.println("Error!");
                })
                .complete((response, context) -> {
                    context.put("value on complete", true);

                    System.out.println("Complete!");
                })
                .execute((response, context) -> {

                    context.entrySet().forEach(entry -> {
                        System.out.println(entry.getKey() + " -" + entry.getValue());
                    });

                    return null;
                });
    }
}
```


## Cookies (Session)

```java
import com.github.nidorx.http.HttpRequest;

import java.net.CookieManager;

public class HttpRequestCookies {

    public static void main(String[] args) throws Exception {

        CookieManager cookieManager = new CookieManager();

        System.out.println("Before 1# request");
        print(cookieManager);

        HttpRequest.build("https://www.google.com")
                .cookieManager(cookieManager)
                .execute();


        System.out.println("After 1# request");
        print(cookieManager);

        // Reuse same cookies
        HttpRequest.build("https://www.google.com")
                .cookieManager(cookieManager)
                .execute();

        System.out.println("After 2# request");
        print(cookieManager);

    }

    private static void print(CookieManager cookieManager) {
        cookieManager.getCookieStore().getCookies().forEach(cookie -> {
            System.out.println(cookie.toString());
        });
    }
}
```


## Basic Authentication

```java
import com.github.nidorx.http.HttpRequest;

import java.util.Map;

public class HttpRequestAuthBasic {

    public static void main(String[] args) throws Exception {

        String username = "nidorx";
        String password = "MY_PASSWORD";

        // https://developer.github.com/v3/auth/#basic-authentication
        Map<String, Object> user = HttpRequest.build("https://api.github.com/user")
                .authBasic(username, password)
                .path("USER", username)
                .execute()
                .fromJson();

        System.out.println(user);
    }
}

```

## More ...

Press `Ctrl + Space` on your IDE
