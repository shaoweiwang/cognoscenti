package org.socialbiz.cog;

import org.socialbiz.cog.exception.NGException;
import org.socialbiz.cog.exception.ProgramLogicError;
import java.util.Vector;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class NGSection extends DOMFace
{

    public NGPage parent = null;
    public SectionDef def = null;

    public NGSection(Document d, Element e, DOMFace p)
        throws Exception
    {
        super(d, e, p);

        //make sure that these members are present, a lot of logic requres them
        //to be there.
        if (e==null)
        {
            throw new ProgramLogicError("someone created a section object, but passed null for element");
        }
        if (p==null)
        {
            throw new ProgramLogicError("someone created a section object, but passed null for page object");
        }

        parent = (NGPage) p;

        String sectionName = getAttribute("name");
        if (sectionName==null || sectionName.length()==0)
        {
            //Sections must be constructed with a name value specified in advance.
            //because sections are identified by name.  Only one section per name allowed.
            throw new ProgramLogicError("Section tag MUST have an attribute 'name' with a valid value.");
        }
        def = SectionDef.getDefByName(sectionName);
        if (def==null)
        {
            throw new NGException("nugen.exception.unable.to.find.definition",new Object[]{sectionName});
        }
        assertNameIsConsistent();
    }


    // testing code, can be removed
    private void assertNameIsConsistent()
        throws Exception
    {
        //just checking here for correctness
        String myName = getAttribute("name");
        if (myName==null)
        {
            throw new ProgramLogicError("Somehow the name is null on this section");
        }
        String defName = def.getTypeName();
        if (!myName.equals(defName))
        {
            throw new ProgramLogicError("Was not able to find the right definition for '"
                  +myName+"' and got '"+defName+"' instead.");
        }
    }


    public String getName()
        throws Exception
    {
        String name = getAttribute("name");
        if (name==null)
        {
            throw new ProgramLogicError("Somehow the name is null on this section");
        }
        assertNameIsConsistent();
        return name;
    }


    /**
    * Get the 'value' of this section as text.
    * Only for simple wiki value sections
    */
    public String asText()
        throws Exception
    {
        if (!getFormat().isJustText())
        {
            throw new RuntimeException("Oops, asked to get text ilegally from "+getName()+" which is type "+getFormat().getName());
        }

        // Added June 2009
        // Need for migration.  Originally the wiki
        // source was placed directly in the section tag, while for other
        // sections there were elements in the section tag.  This is
        // inconsistent.  The change is to move the text into a sub element
        // named wiki.
        Element wikiElem = DOMUtils.getChildElement(fEle, "wiki");
        if (wikiElem!=null)
        {
            return getScalar("wiki");
        }
        String value = DOMUtils.textValueOf(fEle, false);
        if (value==null)
        {
            value="";
        }
        setText(value, null);
        return value;

    }


    /**
    * Set the 'value' of this section as text.
    * Only for simple wiki value sections
    */
    public void setText(String textValue, AuthRequest ar)
        throws Exception
    {
        //setting the text on the wrong type of section would be bad
        if (!getFormat().isJustText())
        {
            throw new RuntimeException("Oops, asked to get text ilegally from "+getName()+" which is type "+getFormat().getName());
        }
        if (fEle==null)
        {
            throw new RuntimeException("Why is the fEle variable null?????");
        }

        setScalar("wiki", textValue);
        if (ar!=null)
        {
            setLastModify(ar);
        }
    }


    public SectionFormat getFormat()
    {
        return def.getFormat();
    }

    /**
    * Returns 'true' is the section def is deprecated.
    */
    public boolean isDeprecated()
    {
        return def.deprecated;
    }

    /**
    * Returns 'true' is the section has no content at this time.
    */
    public boolean isEmpty() throws Exception
    {
        SectionFormat sf = def.format;
        return sf.isEmpty(this);
    }

    public void findLinks(Vector<String> v)
        throws Exception
    {
        def.format.findLinks(v, this);
    }

    public long getLastModifyTime()
    {
        String timeAttrib = getAttribute("modTime");
        return safeConvertLong(timeAttrib);
    }

    public String getLastModifyUser()
    {
        return getAttribute("modUser");
    }

    public void setLastModify(AuthRequest ar)
    {
        setAttribute("modTime", Long.toString(ar.nowTime));
        setAttribute("modUser", ar.getBestUserId());
    }

    /**
    * What through whatever elements this owns and put all the four digit
    * IDs into the vector so that we can generate another ID and assure it
    * does not duplication any id found here.
    */
    public void findIDs(Vector<String> v) throws Exception {
        def.format.findIDs(v, this);
    }

}
