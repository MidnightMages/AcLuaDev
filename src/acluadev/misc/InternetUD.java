package acluadev.misc;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class InternetUD  extends BaseUDComponent {
    @Override
    protected String getComponentType() {
        return "internet";
    }

    @LuaCallable
    public LuaObject get(String luaUrl) {
        var url = URI.create(luaUrl);
        var req = HttpRequest.newBuilder(url).GET().build();
        //noinspection resource
        var client = HttpClient.newHttpClient();

        try {
            var resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            var rv = LuaObject.table();
            rv.set("status", LuaObject.of(resp.statusCode()));
            rv.set("body", LuaObject.of(resp.body()));
            return rv;
        } catch (IOException | InterruptedException e) {
            System.out.printf("Exception during http request to %s: %s%n", url, e);
            throw new LuaJavaError("An error has occurred, check the log for more information");
        }
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs) {
        // TODO actually provide serializaion
        return null;
    }

    @LuaDeserializer
    public static BiosUD todoDeserializer(LuaObject[] objs, ByteArrayReader reader) {
        // TODO actually provide serializaion
        return null;
    }
}