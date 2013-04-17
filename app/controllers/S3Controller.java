package controllers;

import java.util.List;
import java.util.UUID;

import external.MyConstants;

import models.S3File;
import play.db.ebean.Model;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

public class S3Controller extends Controller {

	public static Result index() {
        List<S3File> uploads = new Model.Finder<UUID, S3File>(UUID.class, S3File.class).all();
        return ok(Json.toJson(uploads));
    }

    public static Result upload() {
        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart uploadFilePart = body.getFile("upload");
        if (uploadFilePart != null) {
            S3File s3File = new S3File();
            s3File.type = MyConstants.S3Strings.SIZE_ORIGINAL.toString();
            s3File.file = uploadFilePart.getFile();
            s3File.save();
            return ok(Json.toJson(s3File));
        }
        else {
            return badRequest("File upload error");
        }
    }
 
}