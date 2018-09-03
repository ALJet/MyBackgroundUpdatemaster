package indi.aljet.mybackgroundupdate_master.bgupdate;


import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Url;
import rx.Observable;

public interface DownloadApi {

    @GET
    Observable<ResponseBody> downloadFile(@Url String filUrl);
}
