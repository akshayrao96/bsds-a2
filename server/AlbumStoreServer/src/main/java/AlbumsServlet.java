import com.google.gson.Gson;
import com.google.gson.JsonObject;
import config.DynamoDBConfig;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import model.Album;
import model.AlbumProfile;
import model.DynamoDBController;
import model.DynamoDBTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

@MultipartConfig
@WebServlet(name = "AlbumsServlet", value = "/albums/*")
public class AlbumsServlet extends HttpServlet {

  private DynamoDBController dbController;
  private final String TABLE_NAME = "AlbumsData";

  @Override
  public void init() throws ServletException {
    super.init();
    DynamoDbClient ddb = DynamoDBConfig.initDBClient();
    try {
      dbController = new DynamoDBController(ddb);
      if (!DynamoDBTable.doesTableExist(ddb, TABLE_NAME)) {
        DynamoDBTable.createTable(ddb, TABLE_NAME);
      }
    } catch (IOException | DynamoDbException e) {
      throw new RuntimeException("Initialization failed: " + e.getMessage(), e);
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    response.setContentType("application/json");
    Gson gson = new Gson();

    String urlPath = request.getPathInfo();

    Album example = new Album("Sex Pistols", "Never Mind The Bollocks!", "1977");

    String[] urlParts = urlPath.split("/");
    if (urlParts.length != 2) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    } else {
//      Album album = dbController.getProfile(urlParts[1]);
//      System.out.println(album);
      response.getOutputStream().print(gson.toJson(example));
      response.setStatus(HttpServletResponse.SC_OK);
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    response.setContentType("application/json");

    Part profilePart = request.getPart("profile");
    JsonObject profileObject = null;

    if (profilePart != null) {
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(profilePart.getInputStream()))) {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
          stringBuilder.append(line);
        }
        String profileJson = stringBuilder.toString();
        Gson gson = new Gson();
        profileObject = gson.fromJson(profileJson, JsonObject.class);
      }
    }

    AlbumProfile albumProfile = null;

    if (profileObject != null) {
      albumProfile = new AlbumProfile(new Album(
          profileObject.get("artist").getAsString(),
          profileObject.get("title").getAsString(),
          profileObject.get("year").getAsString()));
    }

    StringBuilder imgSize = new StringBuilder();
    Part image = request.getPart("image");
    long size = image.getSize();
    imgSize.append(size).append("KB");

    JsonObject jsonObject = new JsonObject();
    assert albumProfile != null;

//    dbController.postProfile(albumProfile);

    jsonObject.addProperty("albumID", albumProfile.getAlbumID());
    jsonObject.addProperty("imageSize", imgSize.toString());

    try (PrintWriter out = response.getWriter()) {
      out.write(jsonObject.toString());
    }
  }
}

