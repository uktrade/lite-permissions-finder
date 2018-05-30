package triage.text;

import java.util.List;

public interface HtmlRenderService {

  //1) loop through nodes in rich text, wrap with <a> tags as required based on node type
  //2) convert "markdown" bullets to HTML lists
  //3) convert line breaks to <br>s
  String convertRichTextToHtml(RichText richText);

  String convertRichTextToHtmlWithoutLinks(RichText richText);

  String convertRichTextToPlainText(RichText richText);

  String createRelatedItemsHtml(List<RichText> richTextList);

  String createDefinitions(List<RichText> richTextList);
}
