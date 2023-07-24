package searchengine.services;

public class UtilService
{
    public static String getUrlWithSlash(String inputUrl)
    {
        String outputUrl;

        if(!inputUrl.endsWith("/"))
        {
            outputUrl = inputUrl.concat("/");
        }
        else
        {
            outputUrl = inputUrl;
        }

        return outputUrl;
    }

    public static String getUrlWithoutSlash(String inputUrl)
    {
        String outputUrl;

        if(inputUrl.endsWith("/"))
        {
            int length = inputUrl.length();
            outputUrl = inputUrl.substring(0, length - 1);
        }
        else
        {
            outputUrl = inputUrl;
        }

        return outputUrl;
    }
}
