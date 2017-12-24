package mg.startapps.kode.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kevinRabefaritra on 08/02/17.
 */
public class KodeList<K extends KodeObject> extends ArrayList<K>
{
    public KodeList()
    {
        super();
    }

    public KodeList(List<K> values)
    {
        super();
        for(K object : values)
        {
            this.add(object);
        }
    }
}
